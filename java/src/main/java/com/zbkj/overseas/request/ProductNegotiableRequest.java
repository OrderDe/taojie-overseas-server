package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch negotiable toggle for the admin product list: the list flips
 * the price-on-request flag per row or for a multi-selection, so the
 * operator does not have to open the edit dialog for every product.
 */
@Data
@Schema(name = "ProductNegotiableRequest", description = "商品批量面议开关请求对象")
public class ProductNegotiableRequest {

    @Schema(description = "商品 id 列表")
    private List<Long> ids = new ArrayList<>();

    @Schema(description = "是否面议（true 时前台只显示「面议」、不显示数字价格）")
    private Boolean negotiable;
}
