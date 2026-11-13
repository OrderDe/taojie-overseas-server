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
 * Site-wide visitor-statistics row — one per visitor token, fed by the
 * storefront /api/track beacon (views, clicks, heartbeats). Separate
 * from the chat session table: this tracks every visitor, not only the
 * ones who open the chat widget. Same contract as the Node backend.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_visitor")
public class OverseasVisitor implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Random per-visitor token (browser localStorage), unique. */
    private String token;

    private String ip;

    private String country;

    private String region;

    private String city;

    private String timezone;

    /** desktop / mobile / tablet, from the User-Agent. */
    private String device;

    private String browser;

    private String os;

    /** UA looks like a bot/crawler (a signal only — never a hard block). */
    private Boolean isBot;

    /** Page the visitor came from (document.referrer, first sighting). */
    private String referrer;

    private String firstPage;

    private String currentPage;

    private Integer pageViews;

    /** Visits: a new one starts after a gap of more than 30 minutes. */
    private Integer visits;

    private Date firstSeen;

    private Date lastSeen;

    /** Start of the current visit (reset when a new visit begins). */
    private Date visitStartedAt;

    /** Accumulated dwell seconds (per-step accrual capped at 90s). */
    private Integer dwellSec;
}
