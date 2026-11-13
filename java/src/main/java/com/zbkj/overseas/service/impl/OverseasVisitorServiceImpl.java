package com.zbkj.overseas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasChatSessionDao;
import com.zbkj.overseas.dao.OverseasVisitEventDao;
import com.zbkj.overseas.dao.OverseasVisitorDao;
import com.zbkj.overseas.model.OverseasChatSession;
import com.zbkj.overseas.model.OverseasVisitEvent;
import com.zbkj.overseas.model.OverseasVisitor;
import com.zbkj.overseas.request.TrackRequest;
import com.zbkj.overseas.response.VisitEventResponse;
import com.zbkj.overseas.response.VisitorRowResponse;
import com.zbkj.overseas.response.VisitorStatsResponse;
import com.zbkj.overseas.service.OverseasVisitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Visitor-statistics implementation — the Java twin of the Node
 * backend's /api/track + /api/admin/visitors.
 *
 * Definitions (identical to Node, and what the admin page shows):
 * - a "visit" is a run of activity with no gap over 30 minutes; a
 *   longer gap starts a new visit (visits + 1, visitStartedAt reset).
 * - dwell time accrues on every view/heartbeat/click by the gap since
 *   lastSeen, capped at 90s per step; a first sighting and a new visit
 *   add nothing.
 * - "online" = lastSeen within 60 seconds.
 * - "today" = the server's local calendar day.
 * - PV/UV/trend count kind='view' events only; clicks never count.
 */
@Slf4j
@Service
public class OverseasVisitorServiceImpl implements OverseasVisitorService {

    private static final long VISIT_GAP_MS = 30L * 60 * 1000;
    private static final long DWELL_CAP_SEC = 90;
    private static final long ONLINE_MS = 60_000;
    private static final int MAX_TRACK_PER_MINUTE = 120;
    private static final Pattern TOKEN_RE = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");

    @Autowired
    private OverseasVisitorDao visitorDao;

    @Autowired
    private OverseasVisitEventDao eventDao;

    @Autowired
    private OverseasChatSessionDao chatSessionDao;

    private final ConcurrentHashMap<String, long[]> trackHits = new ConcurrentHashMap<>();

    @Override
    public String track(TrackRequest request, String clientIp, String userAgent) {
        checkRateLimit(clientIp, MAX_TRACK_PER_MINUTE);
        String type = request == null ? "" : trimTo(request.getType(), 16);
        if (!type.equals("heartbeat") && !type.equals("click")) {
            type = "view";
        }
        String token = request == null ? "" : trimTo(request.getToken(), 128);
        if (!TOKEN_RE.matcher(token).matches()) {
            byte[] raw = new byte[24];
            new SecureRandom().nextBytes(raw);
            token = HexFormat.of().formatHex(raw);
        }
        String page = request == null ? "" : trimTo(request.getPage(), 300);
        String action = request == null ? "" : trimTo(request.getAction(), 200);
        Date now = new Date();
        ChatProfileUtil.UaInfo ua = ChatProfileUtil.parseUserAgent(userAgent);
        boolean local = ChatProfileUtil.isPrivateIp(clientIp);

        OverseasVisitor visitor = findByToken(token);
        if (visitor == null) {
            visitor = new OverseasVisitor();
            visitor.setToken(token);
            visitor.setIp(clientIp == null ? "" : clientIp);
            visitor.setCountry(local ? "本地/内网" : "");
            visitor.setRegion("");
            visitor.setCity("");
            visitor.setTimezone("");
            visitor.setDevice(ua.device());
            visitor.setBrowser(ua.browser());
            visitor.setOs(ua.os());
            visitor.setIsBot(ua.suspectBot());
            visitor.setReferrer(request == null ? "" : trimTo(request.getReferrer(), 500));
            visitor.setFirstPage(page);
            visitor.setCurrentPage(page);
            visitor.setPageViews(type.equals("view") ? 1 : 0);
            visitor.setVisits(1);
            visitor.setFirstSeen(now);
            visitor.setLastSeen(now);
            visitor.setVisitStartedAt(now);
            visitor.setDwellSec(0);
            visitorDao.insert(visitor);
            if (type.equals("view") || type.equals("click")) {
                insertEvent(token, type, page, type.equals("click") ? action : "", now);
            }
            if (!local) {
                resolveGeo(visitor);
            }
            return token;
        }

        // Existing visitor: visit/dwell bookkeeping first.
        long lastMs = visitor.getLastSeen() == null ? 0 : visitor.getLastSeen().getTime();
        if (lastMs == 0 || now.getTime() - lastMs > VISIT_GAP_MS) {
            visitor.setVisits((visitor.getVisits() == null ? 0 : visitor.getVisits()) + 1);
            visitor.setVisitStartedAt(now);
        } else {
            long deltaSec = Math.min(DWELL_CAP_SEC, Math.max(0, (now.getTime() - lastMs) / 1000));
            visitor.setDwellSec((visitor.getDwellSec() == null ? 0 : visitor.getDwellSec()) + (int) deltaSec);
        }
        boolean ipChanged = clientIp != null && !clientIp.isBlank() && !clientIp.equals(visitor.getIp());
        if (ipChanged) {
            visitor.setIp(clientIp);
        }
        visitor.setDevice(ua.device());
        visitor.setBrowser(ua.browser());
        visitor.setOs(ua.os());
        visitor.setIsBot(ua.suspectBot());
        if ((visitor.getReferrer() == null || visitor.getReferrer().isEmpty()) && request != null) {
            visitor.setReferrer(trimTo(request.getReferrer(), 500));
        }
        if (!page.isEmpty()) {
            visitor.setCurrentPage(page);
        }
        visitor.setLastSeen(now);
        if (type.equals("view")) {
            visitor.setPageViews((visitor.getPageViews() == null ? 0 : visitor.getPageViews()) + 1);
        }
        visitorDao.updateById(visitor);
        if (type.equals("view")) {
            insertEvent(token, "view", page, "", now);
        } else if (type.equals("click")) {
            insertEvent(token, "click", page.isEmpty() ? strOrEmpty(visitor.getCurrentPage()) : page, action, now);
        }
        if (!local && (ipChanged || visitor.getCountry() == null || visitor.getCountry().isEmpty())) {
            resolveGeo(visitor);
        }
        return token;
    }

    @Override
    public VisitorStatsResponse stats() {
        Date now = new Date();
        VisitorStatsResponse.Summary summary = new VisitorStatsResponse.Summary();
        Date onlineSince = new Date(now.getTime() - ONLINE_MS);
        summary.setOnline(toInt(visitorDao.selectCount(new LambdaQueryWrapper<OverseasVisitor>()
                .ge(OverseasVisitor::getLastSeen, onlineSince))));
        Map<String, Object> today = dayCounts(0);
        summary.setTodayUv(toInt(today.get("uv")));
        summary.setTodayPv(toInt(today.get("pv")));
        summary.setTotalUv(toInt(visitorDao.selectCount(null)));
        List<Map<String, Object>> sums = visitorDao.selectMaps(new QueryWrapper<OverseasVisitor>()
                .select("COALESCE(SUM(page_views), 0) AS pv"));
        summary.setTotalPv(sums.isEmpty() ? 0 : toInt(sums.get(0).get("pv")));

        VisitorStatsResponse stats = new VisitorStatsResponse();
        stats.setSummary(summary);
        stats.setCountries(countries());
        List<VisitorStatsResponse.TrendDay> trend = new ArrayList<>();
        for (int back = 6; back >= 0; back--) {
            Map<String, Object> day = dayCounts(-back);
            VisitorStatsResponse.TrendDay d = new VisitorStatsResponse.TrendDay();
            d.setDate(LocalDate.now().plusDays(-back).toString());
            d.setUv(toInt(day.get("uv")));
            d.setPv(toInt(day.get("pv")));
            trend.add(d);
        }
        stats.setTrend(trend);
        List<OverseasVisitor> rows = visitorDao.selectList(new LambdaQueryWrapper<OverseasVisitor>()
                .orderByDesc(OverseasVisitor::getLastSeen).last("LIMIT 100"));
        stats.setVisitors(rows.stream().map(v -> toRow(v, now)).collect(Collectors.toList()));
        return stats;
    }

    @Override
    public List<VisitEventResponse> events(String token, int limit) {
        if (findByToken(token) == null) {
            throw new OverseasException(OverseasResultCode.VISITOR_NOT_FOUND);
        }
        int n = Math.min(200, Math.max(1, limit));
        return eventDao.selectList(new LambdaQueryWrapper<OverseasVisitEvent>()
                        .eq(OverseasVisitEvent::getToken, token)
                        .orderByDesc(OverseasVisitEvent::getId)
                        .last("LIMIT " + n))
                .stream().map(e -> {
                    VisitEventResponse r = new VisitEventResponse();
                    r.setId(e.getId());
                    r.setKind(e.getKind());
                    r.setPage(strOrEmpty(e.getPage()));
                    r.setAction(strOrEmpty(e.getAction()));
                    r.setCreatedAt(e.getCreateTime());
                    return r;
                }).collect(Collectors.toList());
    }

    // ---- helpers ----

    private OverseasVisitor findByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return visitorDao.selectOne(new LambdaQueryWrapper<OverseasVisitor>()
                .eq(OverseasVisitor::getToken, token).last("LIMIT 1"));
    }

    private void insertEvent(String token, String kind, String page, String action, Date now) {
        OverseasVisitEvent e = new OverseasVisitEvent();
        e.setToken(token);
        e.setKind(kind);
        e.setPage(page == null ? "" : page);
        e.setAction(action == null ? "" : action);
        e.setCreateTime(now);
        eventDao.insert(e);
    }

    /** PV/UV of view events on the local calendar day `offsetDays` from today. */
    private Map<String, Object> dayCounts(int offsetDays) {
        LocalDate day = LocalDate.now().plusDays(offsetDays);
        ZoneId zone = ZoneId.systemDefault();
        Date from = Date.from(day.atStartOfDay(zone).toInstant());
        Date to = Date.from(day.plusDays(1).atStartOfDay(zone).toInstant());
        List<Map<String, Object>> rows = eventDao.selectMaps(new QueryWrapper<OverseasVisitEvent>()
                .select("COUNT(*) AS pv", "COUNT(DISTINCT token) AS uv")
                .eq("kind", "view")
                .ge("create_time", from)
                .lt("create_time", to));
        return rows.isEmpty() ? Map.of("pv", 0, "uv", 0) : rows.get(0);
    }

    private List<VisitorStatsResponse.CountryCount> countries() {
        return visitorDao.selectMaps(new QueryWrapper<OverseasVisitor>()
                        .select("CASE WHEN country = '' THEN '未知' ELSE country END AS country",
                                "COUNT(*) AS visitors")
                        .groupBy("country")
                        .orderByDesc("visitors")
                        .last("LIMIT 15"))
                .stream().map(m -> {
                    VisitorStatsResponse.CountryCount c = new VisitorStatsResponse.CountryCount();
                    c.setCountry(String.valueOf(m.get("country")));
                    c.setVisitors(toInt(m.get("visitors")));
                    return c;
                }).collect(Collectors.toList());
    }

    private VisitorRowResponse toRow(OverseasVisitor v, Date now) {
        VisitorRowResponse r = new VisitorRowResponse();
        r.setToken(v.getToken());
        r.setIp(strOrEmpty(v.getIp()));
        r.setCountry(strOrEmpty(v.getCountry()));
        r.setRegion(strOrEmpty(v.getRegion()));
        r.setCity(strOrEmpty(v.getCity()));
        r.setTimezone(strOrEmpty(v.getTimezone()));
        r.setDevice(strOrEmpty(v.getDevice()));
        r.setBrowser(strOrEmpty(v.getBrowser()));
        r.setOs(strOrEmpty(v.getOs()));
        r.setSuspectBot(Boolean.TRUE.equals(v.getIsBot()));
        r.setReferrer(strOrEmpty(v.getReferrer()));
        r.setFirstPage(strOrEmpty(v.getFirstPage()));
        r.setCurrentPage(strOrEmpty(v.getCurrentPage()));
        r.setPageViews(v.getPageViews() == null ? 0 : v.getPageViews());
        r.setVisits(v.getVisits() == null ? 0 : v.getVisits());
        r.setFirstSeen(v.getFirstSeen());
        r.setLastSeen(v.getLastSeen());
        long lastMs = v.getLastSeen() == null ? 0 : v.getLastSeen().getTime();
        boolean online = lastMs > 0 && now.getTime() - lastMs < ONLINE_MS;
        r.setOnline(online);
        r.setVisitStartedAt(v.getVisitStartedAt());
        r.setDwellSec(v.getDwellSec() == null ? 0 : v.getDwellSec());
        long startedMs = v.getVisitStartedAt() == null ? lastMs : v.getVisitStartedAt().getTime();
        long endMs = online ? now.getTime() : lastMs;
        r.setCurrentDwellSec((int) Math.max(0, (endMs - startedMs) / 1000));
        return r;
    }

    /**
     * Geo resolution with the same-IP cache first (another visitor row,
     * then a chat session), exactly like Node: only a cache miss goes to
     * ip-api, asynchronously, and failures leave the fields empty.
     */
    private void resolveGeo(OverseasVisitor visitor) {
        String ip = visitor.getIp();
        if (ip == null || ip.isEmpty() || ChatProfileUtil.isPrivateIp(ip)) {
            return;
        }
        OverseasVisitor cached = visitorDao.selectOne(new LambdaQueryWrapper<OverseasVisitor>()
                .eq(OverseasVisitor::getIp, ip)
                .ne(OverseasVisitor::getCountry, "")
                .ne(OverseasVisitor::getToken, visitor.getToken())
                .last("LIMIT 1"));
        if (cached != null) {
            visitor.setCountry(cached.getCountry());
            visitor.setRegion(cached.getRegion());
            visitor.setCity(cached.getCity());
            visitor.setTimezone(cached.getTimezone());
            visitorDao.updateById(visitor);
            return;
        }
        OverseasChatSession chatCached = chatSessionDao.selectOne(new LambdaQueryWrapper<OverseasChatSession>()
                .eq(OverseasChatSession::getIp, ip)
                .ne(OverseasChatSession::getCountry, "")
                .ne(OverseasChatSession::getCountry, "本地/内网")
                .last("LIMIT 1"));
        if (chatCached != null) {
            visitor.setCountry(chatCached.getCountry());
            visitor.setRegion(strOrEmpty(chatCached.getRegion()));
            visitor.setCity(strOrEmpty(chatCached.getCity()));
            visitor.setTimezone(strOrEmpty(chatCached.getTimezone()));
            visitorDao.updateById(visitor);
            return;
        }
        Long id = visitor.getId();
        CompletableFuture.runAsync(() -> {
            ChatProfileUtil.GeoInfo geo = ChatProfileUtil.lookupGeo(ip);
            if (!geo.country().isEmpty()) {
                OverseasVisitor fresh = visitorDao.selectById(id);
                if (fresh != null && (fresh.getCountry() == null || fresh.getCountry().isEmpty())) {
                    fresh.setCountry(geo.country());
                    fresh.setRegion(geo.region());
                    fresh.setCity(geo.city());
                    fresh.setTimezone(geo.timezone());
                    visitorDao.updateById(fresh);
                }
            }
        });
    }

    /** Fixed-window per-IP limit, same pattern as the chat service. */
    private void checkRateLimit(String ip, int maxPerMinute) {
        String key = ip == null ? "unknown" : ip;
        long now = System.currentTimeMillis();
        long[] slot = trackHits.compute(key, (k, v) -> {
            if (v == null || now - v[0] > 60_000) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        if (slot[1] > maxPerMinute) {
            throw new OverseasException(OverseasResultCode.RATE_LIMITED);
        }
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trimTo(String value, int max) {
        String v = value == null ? "" : value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static String strOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
