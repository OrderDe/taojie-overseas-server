package com.zbkj.overseas.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

/**
 * Chat visitor-profile helpers — the Java twin of the Node backend's UA
 * reader and geo lookup. Zero new dependencies: the UA parser is a small
 * keyword scan, geo uses the JDK HttpClient against ip-api.com (free, no
 * key) and is strictly best-effort.
 */
final class ChatProfileUtil {

    private ChatProfileUtil() {
    }

    /** Parsed view of a User-Agent header. */
    record UaInfo(String device, String browser, String os, boolean suspectBot) {
    }

    static UaInfo parseUserAgent(String uaRaw) {
        String ua = uaRaw == null ? "" : uaRaw;
        String low = ua.toLowerCase(Locale.ROOT);
        boolean suspectBot = ua.isEmpty() || containsAny(low,
                "bot", "crawler", "spider", "headless", "curl", "wget", "python-requests",
                "scrapy", "httpclient", "okhttp", "java/", "go-http", "postman", "insomnia");
        String device = "desktop";
        if (containsAny(low, "ipad", "tablet", "playbook", "silk")
                || (low.contains("android") && !low.contains("mobile"))) {
            device = "tablet";
        } else if (containsAny(low, "mobile", "iphone", "ipod", "android", "windows phone", "blackberry")) {
            device = "mobile";
        }
        String browser = "";
        if (low.contains("edg/")) browser = "Edge";
        else if (low.contains("opr/") || low.contains("opera")) browser = "Opera";
        else if (low.contains("chrome/")) browser = "Chrome";
        else if (low.contains("firefox/")) browser = "Firefox";
        else if (low.contains("safari/")) browser = "Safari";
        else if (low.contains("msie") || low.contains("trident")) browser = "IE";
        String os = "";
        if (low.contains("windows")) os = "Windows";
        else if (low.contains("android")) os = "Android";
        else if (containsAny(low, "iphone", "ipad", "ipod")) os = "iOS";
        else if (low.contains("mac os") || low.contains("macintosh")) os = "macOS";
        else if (low.contains("linux")) os = "Linux";
        return new UaInfo(device, browser, os, suspectBot);
    }

    static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isBlank()) return true;
        if (ip.equals("::1") || ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    /** Geo fields from ip-api.com; every field is "" when the lookup fails. */
    record GeoInfo(String country, String region, String city, String timezone) {
    }

    static GeoInfo lookupGeo(String ip) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(2500))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip
                            + "?fields=status,country,regionName,city,timezone"))
                    .timeout(Duration.ofMillis(2500))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = JSONUtil.parseObj(response.body());
            if (!"success".equals(json.getStr("status"))) {
                return new GeoInfo("", "", "", "");
            }
            return new GeoInfo(
                    strOrEmpty(json.getStr("country")),
                    strOrEmpty(json.getStr("regionName")),
                    strOrEmpty(json.getStr("city")),
                    strOrEmpty(json.getStr("timezone")));
        } catch (Exception e) {
            // Geo is best-effort only: any failure leaves the fields empty.
            return new GeoInfo("", "", "", "");
        }
    }

    /** "path · title" label for the visitor's current page. */
    static String pageLabel(String page, String pageTitle) {
        String p = trimTo(page, 300);
        String t = trimTo(pageTitle, 120);
        return t.isEmpty() ? p : p + " · " + t;
    }

    /** Presence: seen within 45s counts as online. */
    static boolean isOnline(Date lastSeenAt) {
        return lastSeenAt != null && System.currentTimeMillis() - lastSeenAt.getTime() < 45_000;
    }

    /** Typing: stamped within 4s counts as "typing…". */
    static boolean isTyping(Date typingAt) {
        return typingAt != null && System.currentTimeMillis() - typingAt.getTime() < 4_000;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String trimTo(String value, int max) {
        String v = value == null ? "" : value.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static String strOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
