package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Visitor "typing…" signal; the widget throttles this to one call per 2s.
 */
@Data
@Schema(name = "ChatTypingRequest", description = "访客正在输入")
public class ChatTypingRequest {

    @NotBlank(message = "会话 token 不能为空")
    @Schema(description = "会话 token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;
}
