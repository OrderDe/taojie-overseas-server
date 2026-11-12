package com.zbkj.overseas.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Overseas storefront live-chat session.
 * A visitor is identified only by a random token kept in the browser's
 * localStorage — no account. lastAgentReadMsgId records how far the
 * support agent has read, so the admin list can count unread messages.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_chat_session")
public class OverseasChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Random per-visitor credential (the widget's only identity). */
    private String visitorToken;

    private String visitorName;

    private String lang;

    /** Highest chat-message id the agent has seen in this session. */
    private Long lastAgentReadMsgId;

    private Date lastMsgAt;

    private Date createTime;
}
