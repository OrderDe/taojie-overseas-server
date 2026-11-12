package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Visitor heartbeat: keeps the session's online flag warm and reports the
 * page the visitor is currently browsing.
 */
@Data
@Schema(name = "ChatPingRequest", description = "访客聊天心跳")
public class ChatPingRequest {

    @NotBlank(message = "会话 token 不能为空")
    @Schema(description = "会话 token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Schema(description = "当前页面路径（location.pathname），可空")
    private String page;

    @Schema(description = "当前页面标题（document.title），可空")
    private String pageTitle;
}
