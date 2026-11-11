package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasSiteConfig;
import com.zbkj.overseas.request.SiteConfigRequest;

import java.util.Map;

/**
 * Overseas site-config service (key-value, mirrors eb_system_config access pattern).
 */
public interface OverseasSiteConfigService extends IService<OverseasSiteConfig> {

    /** All config as a flat map (keys match pc-config.json fields). */
    Map<String, Object> getConfigMap();

    /** Save a partial update; null fields are skipped. Returns the full map afterwards. */
    Map<String, Object> updateConfig(SiteConfigRequest request);
}
