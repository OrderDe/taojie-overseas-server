package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Product list query — request/ package mirrors tjMall-app's front/request style.
 */
@Data
@Schema(name = "ProductSearchRequest", description = "海外商品列表查询")
public class ProductSearchRequest {

    @Schema(description = "语言 en/zh/es/fr/pt，默认 en")
    private String lang = "en";

    @Schema(description = "关键词（在当前语言及英文名中搜索）")
    private String keyword;

    @Schema(description = "分类 id")
    private String categoryId;

    @Schema(description = "品牌 id（与分类/关键词可叠加）")
    private String brandId;

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 100")
    private Integer limit = 20;
}
