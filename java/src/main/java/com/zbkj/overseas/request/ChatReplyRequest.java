package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent replying to a chat session from the admin console.
 */
@Data
@Schema(name = "ChatReplyRequest", description = "客服回复聊天消息")
public class ChatReplyRequest {

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "回复正文", requiredMode = Schema.RequiredMode.REQUIRED)
    private String body;
}
