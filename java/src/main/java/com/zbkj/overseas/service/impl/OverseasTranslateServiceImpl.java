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
 * never translated at runtime. The default provider is the self-hosted
 * LibreTranslate (LIBRETRANSLATE_URL, default http://127.0.0.1:5000 —
 * see docker-compose.yml). The free MyMemory API is kept as an explicit
 * opt-in fallback: TRANSLATE_PROVIDER=mymemory (TRANSLATE_EMAIL raises
 * its daily quota). A request may override the provider for that call
 * only via `provider: "mymemory" | "libre"` (any other value is a 400);
 * the response echoes the provider used, and MyMemory calls may carry
 * a `meta` map (responseStatus, quotaFinished when the source response
 * had it). There is NO silent fallback: a language the chosen provider
 * does not support lands in `errors` like any other failure. Each
 * language gets a 6s timeout, targets run in parallel, and a single
 * failure lands in `errors`.
 */
@Slf4j
@Service
public class OverseasTranslateServiceImpl implements OverseasTranslateService {

    // Supported target languages (source is always zh). Keep in step with
    // the admin language registry (taojie-overseas-admin/src/utils/langs.js).
    private static final List<String> ALL_TARGETS =
            List.of("en", "es", "fr", "pt", "id", "ms", "vi", "th", "ar", "ru", "de");
    private static final int TIMEOUT_MS = 6000;

    @Value("${TRANSLATE_PROVIDER:libre}")
    private String provider;

    @Value("${TRANSLATE_EMAIL:}")
    private String translateEmail;

    @Value("${LIBRETRANSLATE_URL:http://127.0.0.1:5000}")
    private String libreUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(TIMEOUT_MS))
            .build();

    /** One language's outcome: the translation plus optional MyMemory meta. */
    private record OneResult(String text, Map<String, Object> meta) {
    }

    /**
     * Provider for one request: an explicit request `provider` wins over
     * the environment; absent means the env default. An unknown value is
     * a 400, never a silent default (mirrors the Node implementation).
     */
    private String resolveProvider(String requested) {
        if (StrUtil.isBlank(requested)) {
            return "mymemory".equalsIgnoreCase(provider) ? "mymemory" : "libre";
        }
        String p = requested.trim().toLowerCase();
        if ("mymemory".equals(p) || "libre".equals(p)) {
            return p;
        }
        throw new OverseasException(400, "provider must be \"mymemory\" or \"libre\"");
    }

    @Override
    public TranslateResponse translate(TranslateRequest request) {
        String text = StrUtil.trim(request.getText());
        if (text.length() > 500) {
            throw new OverseasException(400, "text too long (max 500 chars)");
        }
        String useProvider = resolveProvider(request.getProvider());
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

        Map<String, CompletableFuture<OneResult>> futures = new LinkedHashMap<>();
        for (String t : targets) {
            futures.put(t, CompletableFuture.supplyAsync(() -> translateOne(text, t, useProvider)));
        }
        Map<String, String> translations = new LinkedHashMap<>();
        Map<String, Object> meta = null;
        for (Map.Entry<String, CompletableFuture<OneResult>> e : futures.entrySet()) {
            try {
                OneResult r = e.getValue().join();
                translations.put(e.getKey(), r.text());
                if (meta == null && r.meta() != null) {
                    meta = r.meta();
                }
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
        response.setProvider(useProvider);
        response.setMeta(meta);
        return response;
    }

    private OneResult translateOne(String text, String target, String useProvider) {
        if ("mymemory".equals(useProvider)) {
            return translateViaMyMemory(text, target);
        }
        return translateViaLibre(text, target);
    }

    // Sentences MyMemory puts in the translation slot when something is
    // wrong (quota exhausted, bad query, ...). responseStatus normally
    // flags these too, but never let one be stored as a name.
    private static final Pattern MYMEMORY_WARNING = Pattern.compile(
            "MYMEMORY WARNING|YOU USED ALL AVAILABLE FREE TRANSLATIONS|QUERY LENGTH LIMIT EXCEEDED"
                    + "|NO QUERY SPECIFIED|INVALID (TARGET|SOURCE) LANGUAGE|INVALID EMAIL",
            Pattern.CASE_INSENSITIVE);

    private OneResult translateViaMyMemory(String text, String target) {
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
            // Quota exhaustion can also surface as a bare HTTP 429/403
            // with no usable body — an error, never a translation.
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
            String out = data == null ? "" : data.getStr("translatedText");
            if (StrUtil.isBlank(out)) {
                throw new RuntimeException("mymemory: empty result");
            }
            String cleaned = decodeHtmlEntities(out).trim();
            if (MYMEMORY_WARNING.matcher(cleaned).find()) {
                throw new RuntimeException("mymemory: " + cleaned);
            }
            // Optional passthrough for the admin MyMemory query page:
            // only fields the source response actually carries, never
            // invented quota numbers (MyMemory has no quota API).
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("responseStatus", json.getInt("responseStatus", 0));
            Boolean quotaFinished = json.getBool("quotaFinished", null);
            if (quotaFinished == null && data != null) {
                quotaFinished = data.getBool("quotaFinished", null);
            }
            if (quotaFinished != null) {
                meta.put("quotaFinished", quotaFinished);
            }
            return new OneResult(cleaned, meta);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private OneResult translateViaLibre(String text, String target) {
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
            return new OneResult(decodeHtmlEntities(out).trim(), null);
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
