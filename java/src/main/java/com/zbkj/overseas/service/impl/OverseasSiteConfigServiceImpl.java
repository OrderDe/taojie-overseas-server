package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.dao.OverseasSiteConfigDao;
import com.zbkj.overseas.model.OverseasSiteConfig;
import com.zbkj.overseas.request.SiteConfigRequest;
import com.zbkj.overseas.service.OverseasSiteConfigService;
import org.springframework.stereotype.Service;

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
        DEFAULTS.put("whatsapp", "");
        DEFAULTS.put("email", "");
        DEFAULTS.put("currency", "$");
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
        if (request.getShowRfq() != null) {
            put("showRfq", String.valueOf(request.getShowRfq()));
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
        if ("showRfq".equals(key)) {
            return Boolean.parseBoolean(value);
        }
        return value == null ? "" : value;
    }
}
