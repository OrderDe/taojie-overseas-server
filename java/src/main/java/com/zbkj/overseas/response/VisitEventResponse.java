package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * One entry of a visitor's timeline in the admin detail drawer:
 * kind view = a page view (page holds the label), kind click = a
 * recorded click (action holds the description).
 */
@Data
@Schema(name = "VisitEventResponse", description = "访客行程事件")
public class VisitEventResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** view / click */
    private String kind;

    private String page;

    private String action;

    private Date createdAt;
}
