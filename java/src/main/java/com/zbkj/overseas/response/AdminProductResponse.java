package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Product as the admin UI edits it: multi-language names as a map (not the
 * language-resolved string the storefront gets), plus the visibility flag.
 */
@Data
@Schema(name = "AdminProductResponse", description = "海外商品（管理端）")
public class AdminProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String sourceId;

    private String categoryId;

    private String brandId;

    @Schema(description = "多语言商品名 {en, zh, es, fr, pt}")
    private Map<String, String> names;

    private BigDecimal price;

    private BigDecimal otPrice;

    @Schema(description = "是否面议")
    private Boolean negotiable;

    private Integer sales;

    private String unit;

    private Integer moq;

    private String image;

    private String video;

    private String tag;

    @Schema(description = "简介（英文/原文）")
    private String blurb;

    private Map<String, Object> specs;

    private Integer sort;

    @Schema(description = "是否上架")
    private Boolean active;
}
