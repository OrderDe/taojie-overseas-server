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
        DEFAULTS.put("profileUrl", "https://williscarpaint.com/");
        DEFAULTS.put("profileMode", "embed");
        DEFAULTS.put("profileContent", defaultProfileContent());
        DEFAULTS.put("tiktok", "");
        DEFAULTS.put("instagram", "");
        DEFAULTS.put("facebook", "");
        DEFAULTS.put("youtube", "");
        DEFAULTS.put("wechat", "");
    }

    // Starter company-profile content, mirroring the Node backend's
    // DEFAULT_CONFIG: taken from the company's own sfsy profile pages
    // (jieshao_1/jieshao_2, zh+en) so the profile page is populated —
    // and editable in the admin — from day one.
    private static SiteConfigRequest.ProfileContent defaultProfileContent() {
        SiteConfigRequest.ProfileContent content = new SiteConfigRequest.ProfileContent();
        content.setHeroImages(new ArrayList<>(java.util.List.of(
                "https://cdn.taojieyph.com/images/20250908/68ad8de345fce8361.jpg",
                "https://cdn.taojieyph.com/images/20250908/68ad8de345fce8362.jpg",
                "https://cdn.taojieyph.com/images/20250908/68ad8de345fce8363.jpg",
                "https://cdn.taojieyph.com/images/20250908/68ad8de345fce8364.jpg",
                "https://cdn.taojieyph.com/images/20250908/68ad8de345fce8365.jpg")));
        content.setSections(new ArrayList<>(java.util.List.of(
                profileSection(
                        langMap("公司简介", "Company Introduction"),
                        langMap("广州三合涂料科技有限公司是一家高新技术企业，专注于汽车漆和工业漆的研发、生产与销售，拥有超过二十年的涂料生产经验。",
                                "Guangzhou Sanhe Coating Technology Co., Ltd. is a high-tech enterprise, focusing on the research and development, production, and sales of automotive and industrial coatings, with over twenty years of experience in paint production."),
                        new ArrayList<>()),
                profileSection(
                        langMap("产品系列", "Product Series"),
                        new LinkedHashMap<>(),
                        new ArrayList<>(java.util.List.of(
                                langMap("汽车漆", "Automotive Coatings"),
                                langMap("清漆", "Clear Coats"),
                                langMap("固化剂", "Hardeners"),
                                langMap("稀释剂", "Diluents"),
                                langMap("1K2K色漆辅料", "1K2K Color Coatings Auxiliary")))),
                profileSection(
                        langMap("技术研发", "Technology R&D"),
                        langMap("公司高度重视技术研发，建立了专业的研发及售后团队。我们不断引进先进的生产和检测设备，以确保客户产品的高品质。",
                                "The company places great emphasis on technology research and development, establishing a professional R&D and after-sales team. We continuously introduce advanced production and testing equipment to ensure the high quality of our customers' products."),
                        new ArrayList<>()),
                profileSection(
                        langMap("产品优势", "Product Advantages"),
                        new LinkedHashMap<>(),
                        new ArrayList<>(java.util.List.of(
                                langMap("质量优良", "High Quality"),
                                langMap("稳定性好", "Good Stability"),
                                langMap("附着力强", "Strong Adhesion"),
                                langMap("遮盖力好", "Good Coverage"),
                                langMap("耐候性好", "Excellent Weather Resistance"),
                                langMap("光泽饱满", "Full Gloss")))),
                profileSection(
                        langMap("市场地位", "Market Position"),
                        langMap("广州三合涂料已成为国内领先的涂料生产厂家，产品远销全球30多个国家。",
                                "Guangzhou Sanhe Coating has become a leading domestic coating manufacturer, with products sold in over 30 countries worldwide."),
                        new ArrayList<>()),
                profileSection(
                        langMap("生产与研发", "Production and R&D"),
                        langMap("公司重视精湛技术，不断引进先进的涂料生产设备和检测设备。生产包括汽车修补漆、工业漆、机械金属漆等多种高端、质量稳定的涂料。设有研发部门，从原材料检测到生产过程严格控制品质保证。致力于新产品的开发，为客户提供最专业的服务。",
                                "The company values exquisite technology and continuously introduces advanced coating production and testing equipment. We produce a variety of high-end, stable quality coatings, including automotive touch-up paints, industrial coatings, and machinery metal coatings. We have a dedicated R&D department that strictly controls the quality from raw material testing to the production process to ensure quality assurance. We are committed to the development of new products, providing the most professional services to our customers."),
                        new ArrayList<>()))));
        return content;
    }

    private static Map<String, String> langMap(String zh, String en) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("zh", zh);
        map.put("en", en);
        return map;
    }

    private static SiteConfigRequest.ProfileSection profileSection(
            Map<String, String> title, Map<String, String> body, java.util.List<Map<String, String>> bullets) {
        SiteConfigRequest.ProfileSection section = new SiteConfigRequest.ProfileSection();
        section.setTitle(title);
        section.setBody(body);
        section.setBullets(bullets);
        return section;
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
        putIfPresent("profileUrl", request.getProfileUrl());
        // Company-profile mode is a two-value enum; anything else is
        // stored as 'embed' (same contract as the Node backend).
        if (request.getProfileMode() != null) {
            put("profileMode", "custom".equals(request.getProfileMode()) ? "custom" : "embed");
        }
        // Custom profile content is stored as a JSON string, like navItems.
        if (request.getProfileContent() != null) {
            put("profileContent", JSONUtil.toJsonStr(request.getProfileContent()));
        }
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
        if ("profileMode".equals(key)) {
            return "custom".equals(value) ? "custom" : "embed";
        }
        if ("profileContent".equals(key)) {
            if (StrUtil.isBlank(value)) {
                return new SiteConfigRequest.ProfileContent();
            }
            try {
                SiteConfigRequest.ProfileContent parsed =
                        JSONUtil.toBean(value, SiteConfigRequest.ProfileContent.class);
                return parsed == null ? new SiteConfigRequest.ProfileContent() : parsed;
            } catch (Exception e) {
                return new SiteConfigRequest.ProfileContent();
            }
        }
        return value == null ? "" : value;
    }
}
