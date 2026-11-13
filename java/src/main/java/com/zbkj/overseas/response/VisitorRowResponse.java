package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * One visitor row for the admin statistics page — same field names as
 * the Node backend's visitor object.
 */
@Data
@Schema(name = "VisitorRowResponse", description = "访客统计：单个访客")
public class VisitorRowResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private String ip;

    private String country;

    private String region;

    private String city;

    private String timezone;

    private String device;

    private String browser;

    private String os;

    private Boolean suspectBot;

    private String referrer;

    private String firstPage;

    private String currentPage;

    private Integer pageViews;

    private Integer visits;

    private Date firstSeen;

    private Date lastSeen;

    /** lastSeen within 60s. */
    private Boolean online;

    private Date visitStartedAt;

    /** Accumulated dwell seconds across all visits. */
    private Integer dwellSec;

    /** Seconds since the current visit started (to "now" when online, else to lastSeen). */
    private Integer currentDwellSec;
}
