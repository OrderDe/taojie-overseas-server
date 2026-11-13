package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Site-config update (admin). Fields mirror the PC frontend's pc-config.json
 * one-to-one; null fields are left unchanged.
 */
@Data
@Schema(name = "SiteConfigRequest", description = "海外站配置（管理端保存）")
public class SiteConfigRequest {

    @Schema(description = "默认主题 A-H")
    private String defaultTheme;

    @Schema(description = "默认语言 en/zh/es/fr/pt")
    private String defaultLanguage;

    @Schema(description = "首页主标题")
    private String heroTitle;

    @Schema(description = "首页副标题")
    private String heroSubtitle;

    @Schema(description = "顶部公告")
    private String announcement;

    @Schema(description = "是否显示询价入口")
    private Boolean showRfq;

    @Schema(description = "是否显示购物车和订单入口（缺省为关，询盘站定位）")
    private Boolean showCart;

    @Schema(description = "是否显示商品价格（缺省为开；关掉后全站商品按面议展示、不显示价格、不能加购）")
    private Boolean showPrices;

    @Schema(description = "是否显示首页『成为经销商』卡片（缺省为关）")
    private Boolean showDealer;

    @Schema(description = "是否显示首页联系方式卡（热线/WhatsApp/邮箱，无经销商字样，缺省为开）")
    private Boolean showContact;

    @Schema(description = "WhatsApp 联系方式")
    private String whatsapp;

    @Schema(description = "联系邮箱")
    private String email;

    @Schema(description = "币种符号，如 $")
    private String currency;

    @Schema(description = "基准币种（商品价格按此币种录入，前台按汇率换算显示），如 CNY")
    private String baseCurrency;

    @Schema(description = "站点 Logo 图片地址，空 = 前台显示文字店名")
    private String logo;

    @Schema(description = "顶部导航菜单 [{label, link, type, categoryId}]，空数组表示清空")
    private List<NavItem> navItems;

    private Boolean navigationConfigured;

    @Schema(description = "英雄区眉题，覆盖主题名")
    private String heroEyebrow;

    @Schema(description = "经销商卡标题")
    private String dealerTitle;

    @Schema(description = "经销商卡说明")
    private String dealerText;

    @Schema(description = "经销商卡按钮文字")
    private String dealerButton;

    @Schema(description = "出口热线电话")
    private String hotline;

    @Schema(description = "品牌故事标题")
    private String storyTitle;

    @Schema(description = "品牌故事正文")
    private String storyText;

    @Schema(description = "公司简介嵌入网址（前台公司简介页整页嵌入；空 = 显示文字版简介）")
    private String profileUrl;

    @Schema(description = "公司简介显示模式：embed 整页嵌入 profileUrl / custom 图文自定义；非法值按 embed 处理")
    private String profileMode;

    @Schema(description = "公司简介图文自定义内容（profileMode=custom 时前台按当前语言渲染）")
    private ProfileContent profileContent;

    @Schema(description = "TikTok 主页链接（空 = 前台不显示图标）")
    private String tiktok;

    @Schema(description = "Instagram 主页链接")
    private String instagram;

    @Schema(description = "Facebook 主页链接")
    private String facebook;

    @Schema(description = "YouTube 频道链接")
    private String youtube;

    @Schema(description = "微信号（前台点击复制）")
    private String wechat;

    @Schema(description = "顶栏搜索框提示字（多语言 {语言码: 文案}，留空的语言用前台内置默认文案）")
    private Map<String, String> searchPlaceholder;

    @Schema(description = "页脚品牌介绍（多语言 {语言码: 文案}）")
    private Map<String, String> footerBlurb;

    @Schema(description = "页脚说明（多语言 {语言码: 文案}）")
    private Map<String, String> footerNote;

    @Schema(description = "页脚版权文案（多语言 {语言码: 文案}，支持 {year} 占位，前台替换为当前年份）")
    private Map<String, String> footerCopyright;

    @Schema(description = "页脚链接列（最多 4 列、每列最多 10 条链接）")
    private List<FooterColumn> footerColumns;

    /** One footer link column: a heading plus its links. */
    @Data
    @Schema(name = "FooterColumn", description = "页脚链接列")
    public static class FooterColumn {

        @Schema(description = "列标题（多语言 {语言码: 文案}）")
        private Map<String, String> title;

        @Schema(description = "链接列表")
        private List<FooterLink> links = new ArrayList<>();
    }

    /** One footer link; jumps exactly like a nav item. */
    @Data
    @Schema(name = "FooterLink", description = "页脚链接")
    public static class FooterLink {

        @Schema(description = "链接文字（多语言 {语言码: 文案}）")
        private Map<String, String> labels;

        @Schema(description = "点击行为：category 跳分类 / anchor 跳页面区块 / page 打开自定义页面 / external 打开外链；空 = 纯文字")
        private String type;

        @Schema(description = "链接地址（anchor / page / external 时使用）")
        private String link;

        @Schema(description = "type=category 时的目标分类 id；空 = 前台按名称匹配分类，匹配不到显示为纯文字")
        private String categoryId;
    }

    /** One top-navigation entry of the storefront. */
    @Data
    @Schema(name = "NavItem", description = "导航菜单项")
    public static class NavItem {

        @Schema(description = "菜单文字")
        private String label;

        private String labelKey;

        private Boolean enabled;

        @Schema(description = "链接地址（锚点或外链时使用）")
        private String link;

        @Schema(description = "点击行为：category 跳分类 / anchor 跳页面区块 / external 打开外链；空 = 前台按 link 推断")
        private String type;

        @Schema(description = "type=category 时的目标分类 id")
        private String categoryId;
    }

    /** Custom company-profile content: banner + hero carousel + sections + gallery + FAQ + testimonials. */
    @Data
    @Schema(name = "ProfileContent", description = "公司简介图文自定义内容")
    public static class ProfileContent {

        @Schema(description = "页面横幅（标题/副标题/背景图）")
        private ProfileBanner banner;

        @Schema(description = "顶部轮播图 URL 列表")
        private List<String> heroImages = new ArrayList<>();

        @Schema(description = "章节列表")
        private List<ProfileSection> sections = new ArrayList<>();

        @Schema(description = "图库 URL 列表")
        private List<String> gallery = new ArrayList<>();

        @Schema(description = "常见问题列表")
        private List<ProfileFaq> faqs = new ArrayList<>();

        @Schema(description = "客户评价列表")
        private List<ProfileTestimonial> testimonials = new ArrayList<>();
    }

    /** Full-width banner at the top of the company-profile page. */
    @Data
    @Schema(name = "ProfileBanner", description = "公司简介页面横幅")
    public static class ProfileBanner {

        @Schema(description = "主标题（多语言 {语言码: 文案}）")
        private Map<String, String> title;

        @Schema(description = "副标题（多语言 {语言码: 文案}）")
        private Map<String, String> subtitle;

        @Schema(description = "背景图 URL（可空）")
        private String image;
    }

    /** One company-profile section: heading + body paragraphs + bullets + optional image. */
    @Data
    @Schema(name = "ProfileSection", description = "公司简介章节")
    public static class ProfileSection {

        @Schema(description = "标题（多语言 {语言码: 文案}）")
        private Map<String, String> title;

        @Schema(description = "正文（多语言 {语言码: 文案}，空行分段）")
        private Map<String, String> body;

        @Schema(description = "要点列表（每条为多语言 {语言码: 文案}）")
        private List<Map<String, String>> bullets;

        @Schema(description = "章节配图 URL（可空）")
        private String image;
    }

    /** One profile FAQ entry. */
    @Data
    @Schema(name = "ProfileFaq", description = "公司简介常见问题")
    public static class ProfileFaq {

        @Schema(description = "问题（多语言 {语言码: 文案}）")
        private Map<String, String> q;

        @Schema(description = "答案（多语言 {语言码: 文案}，空行分段）")
        private Map<String, String> a;
    }

    /** One customer testimonial on the profile page. */
    @Data
    @Schema(name = "ProfileTestimonial", description = "公司简介客户评价")
    public static class ProfileTestimonial {

        @Schema(description = "客户姓名（纯文本）")
        private String name;

        @Schema(description = "评价内容（多语言 {语言码: 文案}）")
        private Map<String, String> text;
    }
}
