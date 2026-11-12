package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * A full chat thread for the admin console: session header + all messages
 * in order. Fetching it marks the session's visitor messages as read.
 */
@Data
@Schema(name = "ChatThreadResponse", description = "聊天会话详情（管理端）")
public class ChatThreadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private ChatSessionResponse session;

    private List<ChatMessageResponse> messages;
}
