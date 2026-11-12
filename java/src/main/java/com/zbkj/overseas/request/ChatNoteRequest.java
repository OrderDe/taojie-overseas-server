package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Internal note on a chat session — visible to agents only, never sent to
 * the visitor.
 */
@Data
@Schema(name = "ChatNoteRequest", description = "聊天会话内部备注")
public class ChatNoteRequest {

    @Schema(description = "备注内容（空串 = 清除）")
    private String note;
}
