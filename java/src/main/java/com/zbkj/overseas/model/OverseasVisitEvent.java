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
 * One storefront tracking event. kind = 'view' (a page view — the only
 * kind counted in PV/UV/trend) or 'click' (a recorded click, kept for
 * the visitor timeline in the admin console).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_visit_event")
public class OverseasVisitEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String token;

    /** view / click */
    private String kind;

    /** Page label at the time of the event. */
    private String page;

    /** Click description ('' for views), e.g. 点商品:1K141. */
    private String action;

    private Date createTime;
}
