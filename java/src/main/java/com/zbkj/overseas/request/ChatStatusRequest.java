package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Chat session lifecycle change: "open" or "closed". A visitor message to
 * a closed session re-opens it automatically.
 */
@Data
@Schema(name = "ChatStatusRequest", description = "聊天会话状态变更")
public class ChatStatusRequest {

    @Schema(description = "open / closed")
    private String status;
}
