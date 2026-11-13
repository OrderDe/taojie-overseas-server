package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Product row returned to the storefront. Field names deliberately match
 * what the overseas PC frontend already consumes (name/price/otPrice/...),
 * so the Java service is a drop-in replacement for the interim Node service.
 */
@Data
@Schema(name = "ProductResponse", description = "海外商品")
public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String categoryId;

    @Schema(description = "按请求语言解析后的分类名")
    private String category;

    private String brandId;

    @Schema(description = "按请求语言解析后的品牌名")
    private String brand;

    @Schema(description = "按请求语言解析后的商品名")
    private String name;

    @Schema(description = "美元价")
    private BigDecimal price;

    @Schema(description = "美元原价")
    private BigDecimal otPrice;

    @Schema(description = "是否面议（true = 不显示数字价格，显示「面议」）")
    private Boolean negotiable;

    private Integer sales;

    private String unit;

    private Integer moq;

    private String image;

    private String video;

    private java.util.List<String> images;

    private java.util.List<String> videos;

    private String tag;

    @Schema(description = "按请求语言解析后的简介")
    private String description;

    private Map<String, String> specs = new LinkedHashMap<>();
}
