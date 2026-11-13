package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

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
}
