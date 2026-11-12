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

    // ---- Visitor profile (captured at start; geo is best-effort) ----

    private String ip;

    private String country;

    private String region;

    private String city;

    private String timezone;

    /** desktop / mobile / tablet, from the User-Agent. */
    private String device;

    private String browser;

    private String os;

    /** Page the visitor came from (document.referrer). */
    private String referrer;

    /** Latest page the visitor is on: "path · title". */
    private String currentPage;

    private Integer pageViews;

    // ---- Presence / typing / read state ----

    /** Last heartbeat or message time; within 45s counts as online. */
    private Date lastSeenAt;

    /** UA looks like a bot/crawler (a signal only — never a hard block). */
    private Boolean suspectBot;

    private Date visitorTypingAt;

    private Date agentTypingAt;

    /** Highest agent-message id the visitor has pulled (read receipt). */
    private Long visitorReadMsgId;

    /** Internal note — agents only, never sent to the visitor. */
    private String note;

    /** open / closed (a visitor message re-opens a closed session). */
    private String status;

    private Date lastMsgAt;

    private Date createTime;
}
