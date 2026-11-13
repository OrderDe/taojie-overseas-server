package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Start (or reuse) a storefront chat session. Sending back a known token
 * returns the existing session instead of creating a new one.
 */
@Data
@Schema(name = "ChatStartRequest", description = "开启在线聊天会话")
public class ChatStartRequest {

    @Schema(description = "访客称呼，可空（默认「访客」）")
    private String name;

    @Schema(description = "已存在的会话 token（浏览器本地保存），带上则复用原会话")
    private String token;

    @Schema(description = "访客语言 en/zh/es/fr/pt")
    private String lang;

    @Schema(description = "来源页（document.referrer），可空")
    private String referrer;

    @Schema(description = "当前页面路径（location.pathname），可空")
    private String page;

    @Schema(description = "当前页面标题（document.title），可空")
    private String pageTitle;

    @Schema(description = "附带商品快照（点询价打开聊天时传），不传则不改变会话已存商品")
    private ChatProductRequest product;

    @Schema(description = "true = 移除会话已附带的商品（访客在商品卡上点了移除）")
    private Boolean clearProduct;
}
