package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Product create/update (admin). The admin UI sends the full editable
 * product; names is the multi-language map stored in the names column,
 * blurb is a single-language description for phase 1 (same as imports).
 */
@Data
@Schema(name = "ProductSaveRequest", description = "海外商品保存（管理端）")
public class ProductSaveRequest {

    @Schema(description = "tjmall 来源商品 id，可留空（自动生成 manual- 前缀）")
    private String sourceId;

    @Schema(description = "分类 id")
    private String categoryId;

    @Schema(description = "品牌 id，可留空（无品牌）")
    private String brandId;

    @Schema(description = "多语言商品名 {en, zh, es, fr, pt}，en/zh 至少填一个")
    private Map<String, String> names;

    @Schema(description = "美元价")
    private BigDecimal price;

    @Schema(description = "美元原价")
    private BigDecimal otPrice;

    @Schema(description = "是否面议（true = 前台不显示数字价格、仅可询价、不可加购）")
    private Boolean negotiable;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "单位，如 pc / carton")
    private String unit;

    @Schema(description = "起订量 MOQ")
    private Integer moq;

    @Schema(description = "图片地址")
    private String image;

    @Schema(description = "视频地址（mp4/webm/mov，可空）")
    private String video;

    @Schema(description = "角标，如 Hot / New")
    private String tag;

    @Schema(description = "一句话简介")
    private String blurb;

    @Schema(description = "规格参数，如 {\"Mix ratio\":\"2 : 1\"}")
    private Map<String, Object> specs;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "是否上架")
    private Boolean active;
}
