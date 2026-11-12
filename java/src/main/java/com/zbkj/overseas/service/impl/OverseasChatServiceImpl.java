package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasChatMessageDao;
import com.zbkj.overseas.dao.OverseasChatSessionDao;
import com.zbkj.overseas.model.OverseasChatMessage;
import com.zbkj.overseas.model.OverseasChatSession;
import com.zbkj.overseas.request.ChatNoteRequest;
import com.zbkj.overseas.request.ChatPingRequest;
import com.zbkj.overseas.request.ChatReplyRequest;
import com.zbkj.overseas.request.ChatSendRequest;
import com.zbkj.overseas.request.ChatStartRequest;
import com.zbkj.overseas.request.ChatStatusRequest;
import com.zbkj.overseas.request.ChatTypingRequest;
import com.zbkj.overseas.response.ChatMessageResponse;
import com.zbkj.overseas.response.ChatSessionResponse;
import com.zbkj.overseas.response.ChatStartResponse;
import com.zbkj.overseas.response.ChatThreadResponse;
import com.zbkj.overseas.service.OverseasChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Live-chat service implementation.
 * Mirrors the Node backend: per-IP rate windows (10 session starts and 30
 * messages per minute), messages capped at 2000 chars, unread counts from
 * the session's lastAgentReadMsgId watermark. Session rows also carry a
 * visitor profile (IP geo, device/browser/OS from the UA, referrer,
 * current page), presence (lastSeenAt heartbeats), typing stamps and a
 * visitor read watermark for "已读" receipts.
 */
@Slf4j
@Service
public class OverseasChatServiceImpl implements OverseasChatService {

    private static final Set<String> LANGS = Set.of("en", "zh", "es", "fr", "pt");
    private static final int MAX_BODY = 2000;
    private static final int MAX_STARTS_PER_MINUTE = 10;
    private static final int MAX_MESSAGES_PER_MINUTE = 30;

    private final ConcurrentHashMap<String, long[]> startHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> messageHits = new ConcurrentHashMap<>();

    @Autowired
    private OverseasChatSessionDao sessionDao;

    @Autowired
    private OverseasChatMessageDao messageDao;

    @Override
    public ChatStartResponse start(ChatStartRequest request, String clientIp, String userAgent) {
        OverseasChatSession existing = findByToken(request == null ? null : request.getToken());
        if (existing != null) {
            // Reuse: refresh presence, and follow the visitor to a new page.
            existing.setLastSeenAt(new Date());
            String page = request == null ? "" : ChatProfileUtil.pageLabel(request.getPage(), request.getPageTitle());
            if (!page.isEmpty() && !page.equals(existing.getCurrentPage())) {
                existing.setCurrentPage(page);
                existing.setPageViews((existing.getPageViews() == null ? 0 : existing.getPageViews()) + 1);
            }
            sessionDao.updateById(existing);
            return startResponse(existing);
        }
        checkRateLimit(startHits, clientIp, MAX_STARTS_PER_MINUTE);
        String name = request == null ? "" : StrUtil.trimToEmpty(request.getName());
        if (name.length() > 64) {
            name = name.substring(0, 64);
        }
        if (name.isEmpty()) {
            name = "访客";
        }
        byte[] raw = new byte[24];
        new SecureRandom().nextBytes(raw);
        ChatProfileUtil.UaInfo ua = ChatProfileUtil.parseUserAgent(userAgent);
        boolean local = ChatProfileUtil.isPrivateIp(clientIp);
        OverseasChatSession session = new OverseasChatSession();
        session.setVisitorToken(HexFormat.of().formatHex(raw));
        session.setVisitorName(name);
        String lang = request == null ? null : request.getLang();
        session.setLang(lang != null && LANGS.contains(lang) ? lang : "en");
        session.setLastAgentReadMsgId(0L);
        session.setVisitorReadMsgId(0L);
        session.setIp(clientIp == null ? "" : clientIp);
        session.setCountry(local ? "本地/内网" : "");
        session.setRegion("");
        session.setCity("");
        session.setTimezone("");
        session.setDevice(ua.device());
        session.setBrowser(ua.browser());
        session.setOs(ua.os());
        session.setSuspectBot(ua.suspectBot());
        session.setReferrer(trimTo(request == null ? null : request.getReferrer(), 500));
        session.setCurrentPage(request == null ? "" : ChatProfileUtil.pageLabel(request.getPage(), request.getPageTitle()));
        session.setPageViews(1);
        session.setStatus("open");
        session.setNote("");
        Date now = new Date();
        session.setLastSeenAt(now);
        session.setCreateTime(now);
        session.setLastMsgAt(now);
        sessionDao.insert(session);
        if (!local) {
            // Geo is best-effort: fill it in asynchronously so a slow or
            // unreachable lookup never delays session creation.
            Long sessionId = session.getId();
            String ip = session.getIp();
            CompletableFuture.runAsync(() -> {
                ChatProfileUtil.GeoInfo geo = ChatProfileUtil.lookupGeo(ip);
                if (!geo.country().isEmpty()) {
                    OverseasChatSession fresh = sessionDao.selectById(sessionId);
                    if (fresh != null) {
                        fresh.setCountry(geo.country());
                        fresh.setRegion(geo.region());
                        fresh.setCity(geo.city());
                        fresh.setTimezone(geo.timezone());
                        sessionDao.updateById(fresh);
                    }
                }
            });
        }
        return startResponse(session);
    }

    @Override
    public ChatMessageResponse sendVisitorMessage(ChatSendRequest request, String clientIp) {
        OverseasChatSession session = findByToken(request.getToken());
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        String body = StrUtil.trimToEmpty(request.getBody());
        if (body.isEmpty()) {
            throw new OverseasException(OverseasResultCode.CHAT_MESSAGE_EMPTY);
        }
        checkRateLimit(messageHits, clientIp, MAX_MESSAGES_PER_MINUTE);
        // A message to a closed session re-opens it.
        if ("closed".equals(session.getStatus())) {
            session.setStatus("open");
        }
        return appendMessage(session, "visitor", body);
    }

    @Override
    public ChatPullResult listVisitorMessages(String token, long afterId) {
        OverseasChatSession session = findByToken(token);
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        List<ChatMessageResponse> messages = messageDao.selectList(new LambdaQueryWrapper<OverseasChatMessage>()
                        .eq(OverseasChatMessage::getSessionId, session.getId())
                        .gt(OverseasChatMessage::getId, Math.max(0L, afterId))
                        .orderByAsc(OverseasChatMessage::getId))
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
        // Pulling up to the latest message means the visitor has now seen
        // every agent message there is: advance their read watermark.
        OverseasChatMessage lastAgent = messageDao.selectOne(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, session.getId())
                .eq(OverseasChatMessage::getSender, "agent")
                .orderByDesc(OverseasChatMessage::getId)
                .last("LIMIT 1"), false);
        long read = session.getVisitorReadMsgId() == null ? 0L : session.getVisitorReadMsgId();
        if (lastAgent != null && lastAgent.getId() > read) {
            session.setVisitorReadMsgId(lastAgent.getId());
            sessionDao.updateById(session);
        }
        return new ChatPullResult(messages, ChatProfileUtil.isTyping(session.getAgentTypingAt()));
    }

    @Override
    public void ping(ChatPingRequest request) {
        OverseasChatSession session = findByToken(request.getToken());
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        session.setLastSeenAt(new Date());
        String page = ChatProfileUtil.pageLabel(request.getPage(), request.getPageTitle());
        if (!page.isEmpty() && !page.equals(session.getCurrentPage())) {
            session.setCurrentPage(page);
            session.setPageViews((session.getPageViews() == null ? 0 : session.getPageViews()) + 1);
        }
        sessionDao.updateById(session);
    }

    @Override
    public void visitorTyping(ChatTypingRequest request) {
        OverseasChatSession session = findByToken(request.getToken());
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        Date now = new Date();
        session.setVisitorTypingAt(now);
        session.setLastSeenAt(now);
        sessionDao.updateById(session);
    }

    @Override
    public List<ChatSessionResponse> listSessionsForAdmin(String keyword) {
        LambdaQueryWrapper<OverseasChatSession> wrapper = new LambdaQueryWrapper<OverseasChatSession>()
                .orderByDesc(OverseasChatSession::getLastMsgAt)
                .orderByDesc(OverseasChatSession::getId);
        if (StrUtil.isNotBlank(keyword)) {
            // Match the visitor name, or any message body in the session.
            List<Long> hitIds = messageDao.selectList(new LambdaQueryWrapper<OverseasChatMessage>()
                            .like(OverseasChatMessage::getBody, keyword.trim()))
                    .stream().map(OverseasChatMessage::getSessionId).distinct().collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(OverseasChatSession::getVisitorName, keyword.trim());
                if (!hitIds.isEmpty()) {
                    w.or().in(OverseasChatSession::getId, hitIds);
                }
            });
        }
        return sessionDao.selectList(wrapper).stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Override
    public ChatThreadResponse threadForAdmin(Long sessionId) {
        OverseasChatSession session = requireSession(sessionId);
        List<OverseasChatMessage> messages = messageDao.selectList(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, sessionId)
                .orderByAsc(OverseasChatMessage::getId));
        // Opening the thread marks every message in it as read by the agent.
        long maxId = messages.isEmpty() ? 0L : messages.get(messages.size() - 1).getId();
        long read = session.getLastAgentReadMsgId() == null ? 0L : session.getLastAgentReadMsgId();
        if (maxId > read) {
            session.setLastAgentReadMsgId(maxId);
            sessionDao.updateById(session);
        }
        ChatThreadResponse response = new ChatThreadResponse();
        response.setSession(toSessionResponse(session));
        response.setMessages(messages.stream().map(this::toMessageResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    public ChatMessageResponse replyForAdmin(Long sessionId, ChatReplyRequest request) {
        OverseasChatSession session = requireSession(sessionId);
        String body = StrUtil.trimToEmpty(request.getBody());
        if (body.isEmpty()) {
            throw new OverseasException(OverseasResultCode.CHAT_MESSAGE_EMPTY);
        }
        session.setAgentTypingAt(null);
        return appendMessage(session, "agent", body);
    }

    @Override
    public void agentTyping(Long sessionId) {
        OverseasChatSession session = requireSession(sessionId);
        session.setAgentTypingAt(new Date());
        sessionDao.updateById(session);
    }

    @Override
    public void updateNote(Long sessionId, ChatNoteRequest request) {
        OverseasChatSession session = requireSession(sessionId);
        session.setNote(trimTo(request == null ? null : request.getNote(), 2000));
        sessionDao.updateById(session);
    }

    @Override
    public void updateStatus(Long sessionId, ChatStatusRequest request) {
        OverseasChatSession session = requireSession(sessionId);
        String status = request == null ? null : request.getStatus();
        session.setStatus("closed".equals(status) ? "closed" : "open");
        sessionDao.updateById(session);
    }

    // ---- helpers ----

    private OverseasChatSession findByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return sessionDao.selectOne(new LambdaQueryWrapper<OverseasChatSession>()
                .eq(OverseasChatSession::getVisitorToken, token.trim()), false);
    }

    private OverseasChatSession requireSession(Long sessionId) {
        OverseasChatSession session = sessionId == null ? null : sessionDao.selectById(sessionId);
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        return session;
    }

    private ChatMessageResponse appendMessage(OverseasChatSession session, String sender, String body) {
        if (body.length() > MAX_BODY) {
            body = body.substring(0, MAX_BODY);
        }
        OverseasChatMessage message = new OverseasChatMessage();
        message.setSessionId(session.getId());
        message.setSender(sender);
        message.setBody(body);
        message.setCreateTime(new Date());
        messageDao.insert(message);
        session.setLastMsgAt(message.getCreateTime());
        // Sending a message is also a presence signal for the visitor.
        if ("visitor".equals(sender)) {
            session.setLastSeenAt(message.getCreateTime());
        }
        sessionDao.updateById(session);
        return toMessageResponse(message);
    }

    private ChatStartResponse startResponse(OverseasChatSession session) {
        ChatStartResponse response = new ChatStartResponse();
        response.setSessionId(session.getId());
        response.setToken(session.getVisitorToken());
        response.setName(session.getVisitorName());
        return response;
    }

    private ChatSessionResponse toSessionResponse(OverseasChatSession session) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setId(session.getId());
        response.setVisitorName(session.getVisitorName());
        response.setLang(session.getLang());
        OverseasChatMessage last = messageDao.selectOne(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, session.getId())
                .orderByDesc(OverseasChatMessage::getId)
                .last("LIMIT 1"), false);
        response.setLastMessage(last == null ? "" : last.getBody());
        long read = session.getLastAgentReadMsgId() == null ? 0L : session.getLastAgentReadMsgId();
        Long unread = messageDao.selectCount(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, session.getId())
                .eq(OverseasChatMessage::getSender, "visitor")
                .gt(OverseasChatMessage::getId, read));
        response.setUnread(unread == null ? 0 : unread.intValue());
        response.setLastMsgAt(session.getLastMsgAt());
        response.setCreateTime(session.getCreateTime());
        // Visitor profile + presence.
        response.setIp(str(session.getIp()));
        response.setCountry(str(session.getCountry()));
        response.setRegion(str(session.getRegion()));
        response.setCity(str(session.getCity()));
        response.setTimezone(str(session.getTimezone()));
        response.setDevice(str(session.getDevice()));
        response.setBrowser(str(session.getBrowser()));
        response.setOs(str(session.getOs()));
        response.setReferrer(str(session.getReferrer()));
        response.setCurrentPage(str(session.getCurrentPage()));
        response.setPageViews(session.getPageViews() == null ? 0 : session.getPageViews());
        response.setLastSeenAt(session.getLastSeenAt());
        response.setOnline(ChatProfileUtil.isOnline(session.getLastSeenAt()));
        response.setSuspectBot(Boolean.TRUE.equals(session.getSuspectBot()));
        Long sent = messageDao.selectCount(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, session.getId())
                .eq(OverseasChatMessage::getSender, "visitor"));
        response.setMsgCount(sent == null ? 0 : sent.intValue());
        response.setStatus(StrUtil.isBlank(session.getStatus()) ? "open" : session.getStatus());
        response.setNote(str(session.getNote()));
        response.setVisitorReadMsgId(session.getVisitorReadMsgId() == null ? 0L : session.getVisitorReadMsgId());
        response.setVisitorTyping(ChatProfileUtil.isTyping(session.getVisitorTypingAt()));
        response.setAgentTyping(ChatProfileUtil.isTyping(session.getAgentTypingAt()));
        return response;
    }

    private ChatMessageResponse toMessageResponse(OverseasChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSender(message.getSender());
        response.setBody(message.getBody());
        response.setCreateTime(message.getCreateTime());
        return response;
    }

    private String trimTo(String value, int max) {
        String v = value == null ? "" : value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private String str(String value) {
        return value == null ? "" : value;
    }

    private void checkRateLimit(ConcurrentHashMap<String, long[]> hits, String ip, int maxPerMinute) {
        String key = ip == null ? "unknown" : ip;
        long now = System.currentTimeMillis();
        long[] slot = hits.compute(key, (k, v) -> {
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
}
