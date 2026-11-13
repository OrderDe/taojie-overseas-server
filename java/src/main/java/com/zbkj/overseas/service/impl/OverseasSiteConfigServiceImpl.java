package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.dao.OverseasSiteConfigDao;
import com.zbkj.overseas.model.OverseasSiteConfig;
import com.zbkj.overseas.request.SiteConfigRequest;
import com.zbkj.overseas.service.OverseasSiteConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Overseas site-config service implementation.
 * Same defaults as the PC frontend's pc-config.json so a fresh database
 * behaves identically to the file-based setup.
 */
@Service
public class OverseasSiteConfigServiceImpl extends ServiceImpl<OverseasSiteConfigDao, OverseasSiteConfig>
        implements OverseasSiteConfigService {

    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("defaultTheme", "H");
        DEFAULTS.put("defaultLanguage", "en");
        DEFAULTS.put("heroTitle", "");
        DEFAULTS.put("heroSubtitle", "");
        DEFAULTS.put("announcement", "");
        DEFAULTS.put("showRfq", true);
        DEFAULTS.put("showCart", false);
        DEFAULTS.put("showDealer", false);
        DEFAULTS.put("showContact", true);
        DEFAULTS.put("whatsapp", "");
        DEFAULTS.put("email", "");
        DEFAULTS.put("currency", "$");
        DEFAULTS.put("baseCurrency", "CNY");
        DEFAULTS.put("logo", "");
        // Homepage content blocks; empty = storefront uses theme/i18n defaults.
        DEFAULTS.put("navItems", new ArrayList<>());
        DEFAULTS.put("navigationConfigured", false);
        DEFAULTS.put("heroEyebrow", "");
        DEFAULTS.put("dealerTitle", "");
        DEFAULTS.put("dealerText", "");
        DEFAULTS.put("dealerButton", "");
        DEFAULTS.put("hotline", "");
        DEFAULTS.put("storyTitle", "");
        DEFAULTS.put("storyText", "");
        DEFAULTS.put("tiktok", "");
        DEFAULTS.put("instagram", "");
        DEFAULTS.put("facebook", "");
        DEFAULTS.put("youtube", "");
        DEFAULTS.put("wechat", "");
    }

    @Override
    public Map<String, Object> getConfigMap() {
        Map<String, Object> map = new LinkedHashMap<>(DEFAULTS);
        for (OverseasSiteConfig row : list()) {
            if (DEFAULTS.containsKey(row.getConfigKey())) {
                map.put(row.getConfigKey(), parseValue(row.getConfigKey(), row.getConfigValue()));
            }
        }
        return map;
    }

    @Override
    public Map<String, Object> updateConfig(SiteConfigRequest request) {
        putIfPresent("defaultTheme", request.getDefaultTheme());
        putIfPresent("defaultLanguage", request.getDefaultLanguage());
        putIfPresent("heroTitle", request.getHeroTitle());
        putIfPresent("heroSubtitle", request.getHeroSubtitle());
        putIfPresent("announcement", request.getAnnouncement());
        putIfPresent("whatsapp", request.getWhatsapp());
        putIfPresent("email", request.getEmail());
        putIfPresent("currency", request.getCurrency());
        putIfPresent("baseCurrency", request.getBaseCurrency());
        putIfPresent("logo", request.getLogo());
        putIfPresent("heroEyebrow", request.getHeroEyebrow());
        putIfPresent("dealerTitle", request.getDealerTitle());
        putIfPresent("dealerText", request.getDealerText());
        putIfPresent("dealerButton", request.getDealerButton());
        putIfPresent("hotline", request.getHotline());
        putIfPresent("storyTitle", request.getStoryTitle());
        putIfPresent("storyText", request.getStoryText());
        putIfPresent("tiktok", request.getTiktok());
        putIfPresent("instagram", request.getInstagram());
        putIfPresent("facebook", request.getFacebook());
        putIfPresent("youtube", request.getYoutube());
        putIfPresent("wechat", request.getWechat());
        if (request.getShowRfq() != null) {
            put("showRfq", String.valueOf(request.getShowRfq()));
        }
        if (request.getShowCart() != null) {
            put("showCart", String.valueOf(request.getShowCart()));
        }
        if (request.getShowDealer() != null) {
            put("showDealer", String.valueOf(request.getShowDealer()));
        }
        if (request.getShowContact() != null) {
            put("showContact", String.valueOf(request.getShowContact()));
        }
        // navItems is stored as a JSON string, same as the multi-language
        // names elsewhere; an empty list is a real value (clears the nav).
        if (request.getNavigationConfigured() != null) {
            put("navigationConfigured", String.valueOf(request.getNavigationConfigured()));
        }
        if (request.getNavItems() != null) {
            put("navItems", JSONUtil.toJsonStr(request.getNavItems()));
        }
        return getConfigMap();
    }

    private void putIfPresent(String key, String value) {
        if (value != null) {
            put(key, value);
        }
    }

    private void put(String key, String value) {
        OverseasSiteConfig row = new OverseasSiteConfig();
        row.setConfigKey(key);
        row.setConfigValue(StrUtil.trim(value));
        row.setUpdateTime(new Date());
        saveOrUpdate(row);
    }

    private Object parseValue(String key, String value) {
        if ("navigationConfigured".equals(key) || "showRfq".equals(key) || "showCart".equals(key) || "showDealer".equals(key) || "showContact".equals(key)) {
            return Boolean.parseBoolean(value);
        }
        if ("navItems".equals(key)) {
            if (StrUtil.isBlank(value)) {
                return new ArrayList<>();
            }
            try {
                return JSONUtil.toList(value, SiteConfigRequest.NavItem.class);
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return value == null ? "" : value;
    }
}
