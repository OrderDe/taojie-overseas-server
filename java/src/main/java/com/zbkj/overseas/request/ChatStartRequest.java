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
}
