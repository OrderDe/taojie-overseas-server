package com.zbkj.overseas.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Overseas product — a synced copy of a tjmall product (sourceId = tjmall
 * eb_store_product.id), extended with the fields the overseas storefront
 * needs and tjmall does not have: multi-language names, USD prices, MOQ.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_product")
public class OverseasProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Source product id in tjmall (unique; upsert key for the sync job). */
    private String sourceId;

    private String categoryId;

    /** Brand id (eb_overseas_brand.id); blank = no brand. */
    private String brandId;

    /** JSON map lang -> name */
    private String names;

    /** USD price */
    private BigDecimal price;

    /** USD original (strike-through) price */
    private BigDecimal otPrice;

    /** Negotiable price （面议）: true = storefront shows no numeric price, enquiry only */
    private Boolean negotiable;

    private Integer sales;

    private String unit;

    private Integer moq;

    private String image;

    /** Product demo video URL ('' = none); mirrors the Node backend's products.video */
    private String video;

    private String tag;

    /** JSON map lang -> description (single-language text allowed for phase 1) */
    private String blurb;

    /** JSON object of spec rows, e.g. {"Mix ratio":"2 : 1"} */
    private String specs;

    /** JSON map lang -> {unit, tag, specs}; source fields remain unchanged. */
    private String localizedContent;

    private Boolean isShow;

    private Integer sort;

    private Date createTime;

    private Date updateTime;
}
