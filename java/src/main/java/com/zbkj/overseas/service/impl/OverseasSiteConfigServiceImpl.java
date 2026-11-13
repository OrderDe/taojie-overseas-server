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
import java.util.List;
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
    // DEFAULT_CONFIG: the full WILLIS / Meishicai company profile from
    // williscarpaint.com (homepage About Us / Support & Services /
    // OUR FACTORY / FAQ / customer comments, plus the About page's
    // product lineup, ISO and technical-team copy; Guangdong Meishicai
    // Auto Paint Co., Ltd, zh+en) so the profile page is populated —
    // and editable in the admin — from day one.
    private static SiteConfigRequest.ProfileContent defaultProfileContent() {
        SiteConfigRequest.ProfileContent content = new SiteConfigRequest.ProfileContent();
        content.setHeroImages(new ArrayList<>(java.util.List.of(
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779616076263.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779623142310.png",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779607313419.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779609121611.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779615218098.jpg"
        )));
        content.setSections(new ArrayList<>(java.util.List.of(
                profileSection(
                        langMap("关于我们", "About Us"),
                        langMap("广东美诗彩汽车漆有限公司位于广东省韶关市，环境优雅，是一家专注于汽车涂料与工业涂料研发、生产、销售及进出口贸易的现代化科技企业，拥有 15 年以上的全球出口经验。\n\n公司拥有先进的设备、高效的检测体系以及完善的售后服务体系，确保产品与服务的品质。\n\n公司已通过 ISO9001 国际质量管理体系认证与 ISO14001 国际环境管理体系认证，是汽车涂料与工业涂料领域的大型专业制造商。\n\n我们拥有一支在该领域拥有 15 年以上经验的专业技术团队，欢迎世界各地的客户访问我们的网站，如有任何需求请随时告知，我们期待为您提供优质的服务。", "Guangdong Meishicai auto paint Co., LTD. located in the elegant environment of Guangdong Province, Shaoguan City, we are a modern technology enterprise specializing in the research and development, production, sales, import and export trade of automotive coatings and industrial coatings, and has over 15 years of experience in exporting worldwide.\n\nWe have advanced equipment, effective testing systems, and a comprehensive after-sales service system to ensure the quality of our products and services.\n\nWe have obtained ISO9001 international quality management system certification and ISO14001 international environmental management agency certification. We are a large professional manufacturer of automotive coatings and industrial coatings.\n\nWe have professional technician team all in this area more than 15 years, welcome customers from all over the world visit our website, and inform us if you have any needs, we are looking forward to give you excellent service."),
                        new ArrayList<>(),
                        ""),
                profileSection(
                        langMap("产品系列", "Product Lineup"),
                        langMap("公司主营产品包括清漆、固化剂、稀释剂、1K 与 2K 素色漆、1K 珍珠漆、1K 水晶珍珠漆、1K 银粉漆、原子灰、1K 与 2K 底漆及其他辅料。", "Our main product lineup includes clear coats, hardeners, thinners, 1K & 2K solid color paints, 1K pearl paints, 1K crystal pearl paints, 1K silver paints, body filler, 1K & 2K primers and other auxiliary materials."),
                        new ArrayList<>(java.util.List.of(
                                langMap("清漆", "Clear coat"),
                                langMap("固化剂", "Hardener"),
                                langMap("稀释剂", "Thinner"),
                                langMap("1K 与 2K 素色漆", "1K & 2K solid color paints"),
                                langMap("1K 珍珠漆与水晶珍珠漆", "1K pearl & crystal pearl paints"),
                                langMap("1K 银粉漆", "1K silver paints"),
                                langMap("原子灰", "Body filler"),
                                langMap("1K 与 2K 底漆", "1K & 2K primers")
                        )),
                        ""),
                profileSection(
                        langMap("出口市场与服务", "Support & Services"),
                        langMap("我们的产品出口到欧洲、美国、韩国、日本、中东、非洲及世界各地，并已与众多合作伙伴建立了长期、稳定的合作关系。我们可以提供 OEM/ODM 服务，欢迎定制设计。我们坚信，严格的质量控制与卓越的服务，是支撑我们不断走得更远、最终实现目标的精神所在。", "We export products to Europe, the USA, South Korea, Japan, the Middle East, Africa and all around the world. We have already established long, stable relationships with many cooperative partners. We can offer OEM/ODM services. Custom design is welcome. We do believe that strict quality control and remarkable service is the spirit to keep us running further and further, and finally realize our goals."),
                        new ArrayList<>(java.util.List.of(
                                langMap("交期可协商：3–7 天", "Negotiate the delivery time: 3-7 days"),
                                langMap("专业售后团队，24 小时服务", "Professional after-sales team, 24H service")
                        )),
                        ""),
                profileSection(
                        langMap("质量与认证", "Quality & Certifications"),
                        langMap("公司配备先进的生产设施与可靠的检测体系，并建有完善的售后服务框架，持续保障卓越的产品品质与令人满意的服务。公司已通过 ISO9001 质量管理体系认证与 ISO14001 环境管理体系认证。", "Equipped with advanced production facilities and reliable testing systems, backed by a comprehensive after-sales service framework, we consistently guarantee premium product quality and satisfactory service. The company has obtained ISO9001 Quality Management System Certification and ISO14001 Environmental Management System Certification."),
                        new ArrayList<>(),
                        ""),
                profileSection(
                        langMap("技术团队与服务", "Technical Team & Support"),
                        langMap("作为涂料行业的大型专业制造商，公司拥有一支具备 10 年以上行业经验的技术团队。热忱欢迎全球客户浏览官网并随时垂询，我们随时准备为您提供优质服务。", "As a large-scale professional manufacturer in the coatings industry, we have a technical team with more than 10 years of industry expertise. We warmly welcome customers worldwide to browse our official website and contact us for any inquiries — we are ready to deliver excellent services for you."),
                        new ArrayList<>(java.util.List.of(
                                langMap("专业客服一对一咨询，充分了解您的需求", "One-on-one consultation with a professional customer service representative to understand your needs"),
                                langMap("长达一年的专属一对一技术支持，现场操作问题由技术团队全程跟进", "Dedicated one-on-one technical support for a full year — all on-site operational issues are handled by our technical team")
                        )),
                        ""),
                profileSection(
                        langMap("我们的工厂", "Our Factory"),
                        langMap("我们拥有专业的生产设备与经验丰富的生产团队。\n\n产品质量有保障，发货速度快！", "WE HAVE PROFESSIONAL PRODUCTION EQUIPMENT AND EXPERIENCED MANUFACTURERS.\n\nTHE QUALITY OF THE PRODUCT IS GUARANTEED, AND THE SHIPMENT SPEED IS FAST!"),
                        new ArrayList<>(),
                        "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894174.jpg"),
                profileSection(
                        langMap("企业概况", "Company Facts"),
                        new LinkedHashMap<>(),
                        new ArrayList<>(java.util.List.of(
                                langMap("主营产品：清漆、稀释剂、固化剂、2K 漆、1K 漆", "Main products: clear coat, thinner, hardener, 2K paint, 1K paint"),
                                langMap("年营业额：500 万 – 1000 万美元", "Annual revenue: US$5 Million – US$10 Million"),
                                langMap("地址：广东省韶关市翁源县", "Location: Wengyuan County, Shaoguan, Guangdong Province, China")
                        )),
                        "")
        )));
        content.setGallery(new ArrayList<>(java.util.List.of(
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894174.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894357.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894497.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894563.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894649.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894850.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626894947.jpg",
                "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/1779626905878.jpg"
        )));
        content.setFaqs(new ArrayList<>(java.util.List.of(
                profileFaq(langMap("如何联系客服？", "How can I contact customer service?"), langMap("如果您对我们的产品或订单有任何疑问，可以通过电子邮件联系我们的客服团队，也可以与我们在线聊天、通过 FB 联系我们，或直接致电！\n\n更多信息，请查看我们的「联系我们」页面。", "If you have any questions about our products or your order, you can contact our customer service team via email, chat with us online, contact us via FB or call us!\n\nFor more information, please see our Contact Us page.")),
                profileFaq(langMap("可以提供免费样品吗？", "Can you provide free samples?"), langMap("可以，我们提供汽车修补漆产品的免费样品，您只需要承担运费。", "Yes, we offer free samples of our automotive refinish paint products. You only need to cover the shipping cost.")),
                profileFaq(langMap("你们是贸易公司还是制造商？", "Are you a trading company or a manufacturer?"), langMap("是的，我们向全球发货。运费另计，将在结账时加上。我们全年都有折扣和促销活动，敬请关注独家优惠。", "Yes, we ship all over the world. Shipping costs will apply, and will be added at checkout. We run discounts and promotions all year, so stay tuned for exclusive deals.")),
                profileFaq(langMap("可以提供定制服务（OEM/ODM）吗？", "Can you offer customized services (OEM/ODM)?"), langMap("可以，我们为汽车修补漆提供 OEM 和 ODM 服务。\n\n我们拥有专门的设计与研发团队，能够满足您在颜色、配方和包装方面的定制需求。\n\nOEM 项目需要品牌授权，自有品牌贴标有 MOQ 要求。\n\n在大规模生产之前，我们可以提供包装 3D 效果图和颜色样品供您确认。", "Yes, we support both OEM and ODM services for automotive refinish paints.\n\nWe have a dedicated design and R&D team to meet your custom color, formulation, and packaging needs.\n\nFor OEM projects, we require brand authorization and have MOQ requirements for private labeling.\n\nWe can provide 3D mockups of packaging and color samples for your approval before mass production.")),
                profileFaq(langMap("你们的 MOQ（最小起订量）是多少？", "What is your MOQ?"), langMap("我们的 MOQ 要求灵活。标准颜色可以接受小批量试单；定制颜色或 OEM 项目的 MOQ 会根据配方和包装需求而有所不同。详情请联系我们。", "We offer flexible MOQ requirements. For standard colors, we can accommodate small trial orders; for custom colors or OEM projects, MOQ will vary based on formulation and packaging needs. Please contact us for details.")),
                profileFaq(langMap("你们在交货前会检测所有货物吗？", "Do you test all your goods before delivery?"), langMap("是的，我们在发货前对所有产品进行 100% 质量检测，确保其符合我们的标准。", "Yes, we conduct 100% quality testing on all products before shipment to ensure they meet our standards.")),
                profileFaq(langMap("你们的付款条件是什么？", "What are your payment terms?"), langMap("我们接受电汇（T/T）付款：预付 30% 定金，70% 尾款在交货前付清。在您结清尾款之前，我们会将成品和包装的照片发送给您确认。", "We accept payment by T/T (Telegraphic Transfer): 30% deposit in advance, and 70% balance before delivery. Before you settle the balance, we will send you photos of the finished products and packaging for your confirmation."))
        )));
        content.setTestimonials(new ArrayList<>(java.util.List.of(
                profileTestimonial("Jack", langMap("产品介绍非常详细，我非常满意。", "The product introduction is very detailed, and I am very satisfied.")),
                profileTestimonial("Parker Robert", langMap("产品质量非常好，客服人员也非常专业。我们很快就收到了产品。期待下一次合作……", "The product quality is very good, and the customer service personnel are also very professional. We quickly received the product. Looking forward to the next collaboration....")),
                profileTestimonial("Kamran Novruzov", langMap("可以配送到阿塞拜疆吗？", "Delivery to Azerbaijan is available?"))
        )));
        return content;
    }

    private static Map<String, String> langMap(String zh, String en) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("zh", zh);
        map.put("en", en);
        return map;
    }

    private static SiteConfigRequest.ProfileSection profileSection(
            Map<String, String> title, Map<String, String> body, java.util.List<Map<String, String>> bullets, String image) {
        SiteConfigRequest.ProfileSection section = new SiteConfigRequest.ProfileSection();
        section.setTitle(title);
        section.setBody(body);
        section.setBullets(bullets);
        section.setImage(image);
        return section;
    }

    private static SiteConfigRequest.ProfileFaq profileFaq(Map<String, String> q, Map<String, String> a) {
        SiteConfigRequest.ProfileFaq faq = new SiteConfigRequest.ProfileFaq();
        faq.setQ(q);
        faq.setA(a);
        return faq;
    }

    private static SiteConfigRequest.ProfileTestimonial profileTestimonial(String name, Map<String, String> text) {
        SiteConfigRequest.ProfileTestimonial testimonial = new SiteConfigRequest.ProfileTestimonial();
        testimonial.setName(name);
        testimonial.setText(text);
        return testimonial;
    }

    // Shape guards for the admin-authored profile content, mirroring the
    // Node backend's sanitizeProfileContent: bound every list and text
    // length so a junk payload can neither break the profile page nor
    // bloat the config row.
    private static SiteConfigRequest.ProfileContent sanitizeProfileContent(SiteConfigRequest.ProfileContent raw) {
        SiteConfigRequest.ProfileContent out = new SiteConfigRequest.ProfileContent();
        if (raw == null) {
            return out;
        }
        out.setHeroImages(sanitizeImageList(raw.getHeroImages()));
        out.setGallery(sanitizeImageList(raw.getGallery()));
        List<SiteConfigRequest.ProfileSection> sections = new ArrayList<>();
        if (raw.getSections() != null) {
            for (SiteConfigRequest.ProfileSection s : raw.getSections()) {
                if (s == null || sections.size() >= 20) {
                    continue;
                }
                SiteConfigRequest.ProfileSection clean = new SiteConfigRequest.ProfileSection();
                clean.setTitle(sanitizeLangMap(s.getTitle(), 300));
                clean.setBody(sanitizeLangMap(s.getBody(), 5000));
                List<Map<String, String>> bullets = new ArrayList<>();
                if (s.getBullets() != null) {
                    for (Map<String, String> b : s.getBullets()) {
                        if (b != null && bullets.size() < 30) {
                            bullets.add(sanitizeLangMap(b, 300));
                        }
                    }
                }
                clean.setBullets(bullets);
                clean.setImage(sanitizeImageUrl(s.getImage()));
                sections.add(clean);
            }
        }
        out.setSections(sections);
        List<SiteConfigRequest.ProfileFaq> faqs = new ArrayList<>();
        if (raw.getFaqs() != null) {
            for (SiteConfigRequest.ProfileFaq f : raw.getFaqs()) {
                if (f != null && faqs.size() < 20) {
                    SiteConfigRequest.ProfileFaq clean = new SiteConfigRequest.ProfileFaq();
                    clean.setQ(sanitizeLangMap(f.getQ(), 300));
                    clean.setA(sanitizeLangMap(f.getA(), 2000));
                    faqs.add(clean);
                }
            }
        }
        out.setFaqs(faqs);
        List<SiteConfigRequest.ProfileTestimonial> testimonials = new ArrayList<>();
        if (raw.getTestimonials() != null) {
            for (SiteConfigRequest.ProfileTestimonial t : raw.getTestimonials()) {
                if (t != null && testimonials.size() < 10) {
                    SiteConfigRequest.ProfileTestimonial clean = new SiteConfigRequest.ProfileTestimonial();
                    clean.setName(truncate(t.getName() == null ? "" : t.getName().trim(), 100));
                    clean.setText(sanitizeLangMap(t.getText(), 1000));
                    testimonials.add(clean);
                }
            }
        }
        out.setTestimonials(testimonials);
        return out;
    }

    private static Map<String, String> sanitizeLangMap(Map<String, String> raw, int maxLen) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, String> e : raw.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String v = e.getValue().trim();
            if (!v.isEmpty()) {
                out.put(e.getKey(), truncate(v, maxLen));
            }
        }
        return out;
    }

    private static List<String> sanitizeImageList(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String u : raw) {
            if (u != null && !u.trim().isEmpty() && out.size() < 30) {
                out.add(truncate(u.trim(), 1000));
            }
        }
        return out;
    }

    private static String sanitizeImageUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String u = truncate(raw.trim(), 1000);
        return u.startsWith("http://") || u.startsWith("https://") || u.startsWith("//") || u.startsWith("/") ? u : "";
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
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
            put("profileContent", JSONUtil.toJsonStr(sanitizeProfileContent(request.getProfileContent())));
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
