package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.request.TranslateRequest;
import com.zbkj.overseas.response.TranslateResponse;
import com.zbkj.overseas.service.OverseasTranslateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mirrors the Node implementation: translation happens once at data-entry
 * time and the results are stored in the names JSON — the storefront is
 * never translated at runtime. Provider is configurable:
 * TRANSLATE_PROVIDER=libre + LIBRETRANSLATE_URL uses a self-hosted
 * LibreTranslate; default is the free MyMemory API (TRANSLATE_EMAIL
 * raises its daily quota). Each language gets a 6s timeout, the four
 * targets run in parallel, and a single failure lands in `errors`.
 */
@Slf4j
@Service
public class OverseasTranslateServiceImpl implements OverseasTranslateService {

    // Supported target languages (source is always zh). Keep in step with
    // the admin language registry (taojie-overseas-admin/src/utils/langs.js).
    // All eleven were live-tested against MyMemory with the bare code as
    // the langpair target (ms and ar included — no regional suffix needed).
    private static final List<String> ALL_TARGETS =
            List.of("en", "es", "fr", "pt", "id", "ms", "vi", "th", "ar", "ru", "de");
    private static final int TIMEOUT_MS = 6000;

    @Value("${TRANSLATE_PROVIDER:mymemory}")
    private String provider;

    @Value("${TRANSLATE_EMAIL:}")
    private String translateEmail;

    @Value("${LIBRETRANSLATE_URL:}")
    private String libreUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(TIMEOUT_MS))
            .build();

    @Override
    public TranslateResponse translate(TranslateRequest request) {
        String text = StrUtil.trim(request.getText());
        if (text.length() > 500) {
            throw new OverseasException(400, "text too long (max 500 chars)");
        }
        List<String> requested = new ArrayList<>();
        if (request.getTargets() != null) {
            for (String t : request.getTargets()) {
                if (StrUtil.isNotBlank(t) && !requested.contains(t)) {
                    requested.add(t);
                }
            }
        }
        if (requested.isEmpty()) {
            requested = ALL_TARGETS;
        }
        List<String> targets = new ArrayList<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (String t : requested) {
            if (ALL_TARGETS.contains(t)) {
                targets.add(t);
            } else {
                // Codes outside the supported list are never fetched.
                errors.put(t, "unsupported");
            }
        }
        if (targets.isEmpty()) {
            throw new OverseasException(502, "translation service unavailable");
        }

        Map<String, CompletableFuture<String>> futures = new LinkedHashMap<>();
        for (String t : targets) {
            futures.put(t, CompletableFuture.supplyAsync(() -> translateOne(text, t)));
        }
        Map<String, String> translations = new LinkedHashMap<>();
        for (Map.Entry<String, CompletableFuture<String>> e : futures.entrySet()) {
            try {
                translations.put(e.getKey(), e.getValue().join());
            } catch (Exception ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                errors.put(e.getKey(), String.valueOf(cause.getMessage()));
            }
        }
        if (translations.isEmpty()) {
            throw new OverseasException(502, "translation service unavailable");
        }
        TranslateResponse response = new TranslateResponse();
        response.setTranslations(translations);
        response.setErrors(errors);
        return response;
    }

    private String translateOne(String text, String target) {
        if ("libre".equalsIgnoreCase(provider) && StrUtil.isNotBlank(libreUrl)) {
            return translateViaLibre(text, target);
        }
        return translateViaMyMemory(text, target);
    }

    private String translateViaMyMemory(String text, String target) {
        try {
            String pair = URLEncoder.encode("zh-CN|" + target, StandardCharsets.UTF_8);
            String url = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8) + "&langpair=" + pair;
            if (StrUtil.isNotBlank(translateEmail)) {
                url += "&de=" + URLEncoder.encode(translateEmail, StandardCharsets.UTF_8);
            }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("mymemory HTTP " + resp.statusCode());
            }
            JSONObject json = JSONUtil.parseObj(resp.body());
            // On quota exhaustion MyMemory still returns 200-shaped JSON but
            // with responseStatus 403 and a warning sentence as the text.
            if (json.getInt("responseStatus", 0) != 200) {
                throw new RuntimeException("mymemory: "
                        + StrUtil.blankToDefault(json.getStr("responseDetails"), "quota or upstream error"));
            }
            JSONObject data = json.getJSONObject("responseData");
            String out = data == null ? null : data.getStr("translatedText");
            if (StrUtil.isBlank(out)) {
                throw new RuntimeException("mymemory: empty result");
            }
            return decodeHtmlEntities(out).trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String translateViaLibre(String text, String target) {
        try {
            String base = libreUrl.replaceAll("/+$", "");
            JSONObject payload = JSONUtil.createObj()
                    .set("q", text)
                    .set("source", "zh")
                    .set("target", target)
                    .set("format", "text");
            String apiKey = System.getenv("LIBRETRANSLATE_" + "API_KEY");
            if (StrUtil.isNotBlank(apiKey)) {
                payload.set("api_key", apiKey);
            }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/translate"))
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSONUtil.toJsonStr(payload)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("libretranslate HTTP " + resp.statusCode());
            }
            String out = JSONUtil.parseObj(resp.body()).getStr("translatedText");
            if (StrUtil.isBlank(out)) {
                throw new RuntimeException("libretranslate: empty result");
            }
            return decodeHtmlEntities(out).trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(\\d+);");
    private static final Pattern HEX_ENTITY = Pattern.compile("&#x([0-9a-fA-F]+);");

    /** MyMemory sometimes returns HTML entities (&#39; etc.) — decode them. */
    static String decodeHtmlEntities(String s) {
        if (s == null) {
            return "";
        }
        String out = s;
        Matcher m = NUMERIC_ENTITY.matcher(out);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    String.valueOf(Character.toChars(Integer.parseInt(m.group(1))))));
        }
        m.appendTail(sb);
        out = sb.toString();
        m = HEX_ENTITY.matcher(out);
        sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    String.valueOf(Character.toChars(Integer.parseInt(m.group(1), 16)))));
        }
        m.appendTail(sb);
        return sb.toString()
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
