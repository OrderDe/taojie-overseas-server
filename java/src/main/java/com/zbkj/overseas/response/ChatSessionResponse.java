package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Chat session row for the admin session list: visitor name, a preview of
 * the latest message and the count of visitor messages the agent has not
 * read yet.
 */
@Data
@Schema(name = "ChatSessionResponse", description = "聊天会话（管理端列表）")
public class ChatSessionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String visitorName;

    private String lang;

    private String lastMessage;

    private Integer unread;

    private Date lastMsgAt;

    private Date createTime;

    // ---- Visitor profile / presence (same field names as the Node API) ----

    private String ip;

    private String country;

    private String region;

    private String city;

    private String timezone;

    private String device;

    private String browser;

    private String os;

    private String referrer;

    private String currentPage;

    private Integer pageViews;

    private Date lastSeenAt;

    private Boolean online;

    private Boolean suspectBot;

    /** Count of messages this visitor has sent. */
    private Integer msgCount;

    private String status;

    private String note;

    private Long visitorReadMsgId;

    private Boolean visitorTyping;

    private Boolean agentTyping;

    /** Product the visitor is asking about (null when none attached). */
    private ChatProductResponse product;
}
