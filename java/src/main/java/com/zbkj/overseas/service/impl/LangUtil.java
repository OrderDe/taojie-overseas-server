package com.zbkj.overseas.service.impl;

import cn.hutool.json.JSONUtil;

import java.util.Map;

/**
 * Multi-language JSON helper. names/blurb columns store {"en":..,"zh":..,...};
 * resolution order: requested lang -> en -> first value -> raw text
 * (a plain non-JSON string is treated as already-resolved text, which lets
 * phase-1 imports carry a single-language description).
 */
final class LangUtil {

    private LangUtil() {
    }

    static String normLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return "en";
        }
        String l = lang.trim().toLowerCase();
        return switch (l) {
            case "en", "zh", "es", "fr", "pt" -> l;
            default -> "en";
        };
    }

    /** The admin edits one description, while MySQL stores a language JSON object. */
    static String withAdminBlurb(String existing, String text) {
        String value = text == null ? "" : text;
        Map<String, String> values = new java.util.LinkedHashMap<>(toStringMap(existing));
        // Preserve every existing translation when the description was not edited.
        if (!values.isEmpty() && value.equals(pick(existing, "en"))) {
            return JSONUtil.toJsonStr(values);
        }
        values.put("en", value);
        values.put("zh", value);
        return JSONUtil.toJsonStr(values);
    }

    static Map<String, Object> localizedContent(String json, String lang) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            cn.hutool.json.JSONObject content = JSONUtil.parseObj(json).getJSONObject(normLang(lang));
            return content == null ? Map.of() : content;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    static String pick(String json, String lang) {
        if (json == null || json.isBlank()) {
            return "";
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("{")) {
            return json;
        }
        try {
            Map<String, Object> map = JSONUtil.toBean(trimmed, Map.class);
            Object v = map.get(normLang(lang));
            if (v == null) {
                v = map.get("en");
            }
            if (v == null && !map.isEmpty()) {
                v = map.values().iterator().next();
            }
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return json;
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> toStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = JSONUtil.toBean(json.trim(), Map.class);
            java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<>();
            raw.forEach((k, v) -> out.put(k, v == null ? "" : String.valueOf(v)));
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }
}
