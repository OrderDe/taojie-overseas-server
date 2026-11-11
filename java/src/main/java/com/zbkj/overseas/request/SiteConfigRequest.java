package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
}
