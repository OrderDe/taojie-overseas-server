package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * One chat message as both the storefront widget and the admin console
 * render it (same shape as the Node backend returns).
 */
@Data
@Schema(name = "ChatMessageResponse", description = "聊天消息")
public class ChatMessageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sessionId;

    /** visitor | agent */
    private String sender;

    private String body;

    private Date createTime;
}
