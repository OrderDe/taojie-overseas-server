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

    @Schema(description = "WhatsApp 联系方式")
    private String whatsapp;

    @Schema(description = "联系邮箱")
    private String email;

    @Schema(description = "币种符号，如 $")
    private String currency;

    @Schema(description = "顶部导航菜单 [{label, link}]，空数组表示清空")
    private List<NavItem> navItems;

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

    /** One top-navigation entry of the storefront. */
    @Data
    @Schema(name = "NavItem", description = "导航菜单项")
    public static class NavItem {

        @Schema(description = "菜单文字")
        private String label;

        @Schema(description = "链接地址")
        private String link;
    }
}
