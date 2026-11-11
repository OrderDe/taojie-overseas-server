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
 * Overseas RFQ (request-for-quotation) lead.
 * Stored only in the overseas database in phase 1 — no write-back to tjmall.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_rfq")
public class OverseasRfq implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String company;

    private String country;

    /** Email / WhatsApp / phone — at least one contact channel is required. */
    private String contact;

    private String productName;

    private String quantity;

    private String message;

    private Date createTime;
}
