package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Rename a chat session's visitor (agents only). The visitor name is a
 * display label on the session row — renaming it helps agents tell
 * repeat visitors apart; the visitor's own token identity is untouched.
 */
@Data
@Schema(name = "ChatVisitorNameRequest", description = "修改聊天访客名称")
public class ChatVisitorNameRequest {

    @Schema(description = "新的访客名称（必填，最长 64 字）")
    private String name;
}
