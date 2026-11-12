package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Visitor sending a chat message; the token identifies the session.
 */
@Data
@Schema(name = "ChatSendRequest", description = "访客发送聊天消息")
public class ChatSendRequest {

    @NotBlank(message = "会话 token 不能为空")
    @Schema(description = "会话 token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息正文", requiredMode = Schema.RequiredMode.REQUIRED)
    private String body;
}
