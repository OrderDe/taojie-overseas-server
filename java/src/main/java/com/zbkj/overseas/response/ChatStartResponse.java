package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Result of starting a chat session: the widget stores the token in
 * localStorage and presents it on every later call.
 */
@Data
@Schema(name = "ChatStartResponse", description = "聊天会话开启结果")
public class ChatStartResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;

    private String token;

    private String name;
}
