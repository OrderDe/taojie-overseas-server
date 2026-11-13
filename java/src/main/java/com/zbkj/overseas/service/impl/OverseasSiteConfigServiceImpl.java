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

    // Default company-profile content, mirroring the Node backend's
    // DEFAULT_CONFIG: the WILLIS / Meishicai company profile from
    // williscarpaint.com's About page (Shaoguan Xinyue Chemical Co.,
    // Ltd, zh+en) so the profile page is populated — and editable in
    // the admin — from day one.
    private static SiteConfigRequest.ProfileContent defaultProfileContent() {
        SiteConfigRequest.ProfileContent content = new SiteConfigRequest.ProfileContent();
        content.setHeroImages(new ArrayList<>(java.util.List.of(
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779616076263.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779623142310.png",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779607313419.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779609121611.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779615218098.jpg")));
        content.setSections(new ArrayList<>(java.util.List.of(
                profileSection(
                        langMap("关于美诗彩", "About Meishicai"),
                        langMap("公司位于广东省韶关市翁源县（韶关新粤化工有限公司），是一家专注于汽车涂料与工业涂料研发、生产、销售及进出口贸易的现代化科技企业，拥有 15 年以上的全球出口经验。",
                                "Nestled in the pleasant surroundings of Shaoguan City — Wengyuan County, Shaoguan, Guangdong Province, China (Shaoguan Xinyue Chemical Co., Ltd) — we are a modern technological enterprise specializing in the R&D, production, sales and import & export trade of automotive coatings and industrial coatings. We boast over 15 years of global export experience."),
                        new ArrayList<>()),
                profileSection(
                        langMap("产品系列", "Product Lineup"),
                        langMap("公司主营产品包括清漆、固化剂、稀释剂、1K 与 2K 素色漆、1K 珍珠漆、1K 水晶珍珠漆、1K 银粉漆、原子灰、1K 与 2K 底漆及其他辅料。",
                                "Our main product lineup includes clear coats, hardeners, thinners, 1K & 2K solid color paints, 1K pearl paints, 1K crystal pearl paints, 1K silver paints, body filler, 1K & 2K primers and other auxiliary materials."),
                        new ArrayList<>(java.util.List.of(
                                langMap("清漆", "Clear coat"),
                                langMap("固化剂", "Hardener"),
                                langMap("稀释剂", "Thinner"),
                                langMap("1K 与 2K 素色漆", "1K & 2K solid color paints"),
                                langMap("1K 珍珠漆与水晶珍珠漆", "1K pearl & crystal pearl paints"),
                                langMap("1K 银粉漆", "1K silver paints"),
                                langMap("原子灰", "Body filler"),
                                langMap("1K 与 2K 底漆", "1K & 2K primers")))),
                profileSection(
                        langMap("质量与认证", "Quality & Certifications"),
                        langMap("公司配备先进的生产设施与可靠的检测体系，并建有完善的售后服务框架，持续保障卓越的产品品质与令人满意的服务。公司已通过 ISO9001 质量管理体系认证与 ISO14001 环境管理体系认证。",
                                "Equipped with advanced production facilities and reliable testing systems, backed by a comprehensive after-sales service framework, we consistently guarantee premium product quality and satisfactory service. The company has obtained ISO9001 Quality Management System Certification and ISO14001 Environmental Management System Certification."),
                        new ArrayList<>()),
                profileSection(
                        langMap("技术团队与服务", "Technical Team & Support"),
                        langMap("作为涂料行业的大型专业制造商，公司拥有一支具备 10 年以上行业经验的技术团队。热忱欢迎全球客户浏览官网并随时垂询，我们随时准备为您提供优质服务。",
                                "As a large-scale professional manufacturer in the coatings industry, we have a technical team with more than 10 years of industry expertise. We warmly welcome customers worldwide to browse our official website and contact us for any inquiries — we are ready to deliver excellent services for you."),
                        new ArrayList<>(java.util.List.of(
                                langMap("专业客服一对一咨询，充分了解您的需求", "One-on-one consultation with a professional customer service representative to understand your needs"),
                                langMap("长达一年的专属一对一技术支持，现场操作问题由技术团队全程跟进", "Dedicated one-on-one technical support for a full year — all on-site operational issues are handled by our technical team")))),
                profileSection(
                        langMap("企业概况", "Company Facts"),
                        new LinkedHashMap<>(),
                        new ArrayList<>(java.util.List.of(
                                langMap("主营产品：清漆、稀释剂、固化剂、2K 漆、1K 漆", "Main products: clear coat, thinner, hardener, 2K paint, 1K paint"),
                                langMap("年营业额：500 万 – 1000 万美元", "Annual revenue: US$5 Million – US$10 Million"),
                                langMap("地址：广东省韶关市翁源县", "Location: Wengyuan County, Shaoguan, Guangdong Province, China")))))));
        content.setGallery(new ArrayList<>(java.util.List.of(
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779624434764.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260525/1779699855501.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779609960810.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779612122478.jpg")));
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
