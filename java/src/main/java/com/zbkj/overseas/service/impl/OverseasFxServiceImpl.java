package com.zbkj.overseas.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.dao.OverseasFxRateDao;
import com.zbkj.overseas.model.OverseasFxRate;
import com.zbkj.overseas.response.FxRatesResponse;
import com.zbkj.overseas.service.OverseasFxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mirrors the Node implementation: rates come from open.er-api.com
 * (free, no API key, ~160 currencies, updated daily) and are cached per
 * base currency in eb_overseas_fx_rate for 12 hours. A failed refresh
 * falls back to the stale cache (flagged {@code stale}) instead of
 * failing the tool — only "no cache + fetch failed" is a 502. All
 * conversion math happens in the admin page.
 */
@Slf4j
@Service
public class OverseasFxServiceImpl implements OverseasFxService {

    private static final long TTL_MS = 12 * 60 * 60 * 1000L;
    private static final int TIMEOUT_MS = 6000;

    @Autowired
    private OverseasFxRateDao fxRateDao;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(TIMEOUT_MS))
            .build();

    @Override
    public FxRatesResponse rates(String base) {
        OverseasFxRate row = fxRateDao.selectById(base);
        FxRatesResponse cached = row == null ? null : toResponse(row, false, true);
        if (cached != null && isFresh(row.getFetchedAt())) {
            return cached;
        }
        try {
            Fetched fresh = fetch(base);
            String fetchedAt = Instant.now().toString();
            OverseasFxRate save = new OverseasFxRate();
            save.setBase(base);
            save.setRates(JSONUtil.toJsonStr(fresh.rates));
            save.setSourceUpdatedAt(fresh.sourceUpdatedAt);
            save.setFetchedAt(fetchedAt);
            if (row == null) {
                fxRateDao.insert(save);
            } else {
                fxRateDao.updateById(save);
            }
            FxRatesResponse out = new FxRatesResponse();
            out.setBase(base);
            out.setRates(fresh.rates);
            out.setSourceUpdatedAt(fresh.sourceUpdatedAt);
            out.setFetchedAt(fetchedAt);
            out.setStale(false);
            out.setFromCache(false);
            return out;
        } catch (Exception e) {
            if (cached != null) {
                cached.setStale(true);
                return cached;
            }
            log.warn("fx rates fetch failed for base {}: {}", base, e.getMessage());
            throw new OverseasException(502, "exchange rate service unavailable");
        }
    }

    private boolean isFresh(String fetchedAtIso) {
        try {
            return Instant.now().toEpochMilli() - Instant.parse(fetchedAtIso).toEpochMilli() < TTL_MS;
        } catch (Exception e) {
            return false;
        }
    }

    private FxRatesResponse toResponse(OverseasFxRate row, boolean stale, boolean fromCache) {
        FxRatesResponse out = new FxRatesResponse();
        out.setBase(row.getBase());
        out.setRates(parseRates(row.getRates()));
        out.setSourceUpdatedAt(row.getSourceUpdatedAt());
        out.setFetchedAt(row.getFetchedAt());
        out.setStale(stale);
        out.setFromCache(fromCache);
        return out;
    }

    private Map<String, Double> parseRates(String json) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        JSONObject obj = JSONUtil.parseObj(json);
        for (String key : obj.keySet()) {
            Double v = obj.getDouble(key);
            if (v != null) {
                out.put(key, v);
            }
        }
        return out;
    }

    private Fetched fetch(String base) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://open.er-api.com/v6/latest/" + base))
                .timeout(Duration.ofMillis(TIMEOUT_MS))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("er-api HTTP " + resp.statusCode());
        }
        JSONObject json = JSONUtil.parseObj(resp.body());
        if (!"success".equals(json.getStr("result")) || json.getJSONObject("rates") == null) {
            throw new RuntimeException("er-api: unexpected response");
        }
        JSONObject ratesObj = json.getJSONObject("rates");
        Map<String, Double> rates = new LinkedHashMap<>();
        for (String key : ratesObj.keySet()) {
            Double v = ratesObj.getDouble(key);
            if (v != null) {
                rates.put(key, v);
            }
        }
        Long unix = json.getLong("time_last_update_unix");
        String updated = unix != null
                ? Instant.ofEpochSecond(unix).toString()
                : String.valueOf(json.getStr("time_last_update_utc", ""));
        Fetched f = new Fetched();
        f.rates = rates;
        f.sourceUpdatedAt = updated;
        return f;
    }

    private static class Fetched {
        private Map<String, Double> rates;
        private String sourceUpdatedAt;
    }
}
