package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch sort save for the admin product list: the list edits sort
 * values inline and submits only the rows that changed.
 */
@Data
@Schema(name = "ProductSortRequest", description = "商品批量排序请求对象")
public class ProductSortRequest {

    @Schema(description = "排序项（商品 id + 新的排序值）")
    private List<Item> items = new ArrayList<>();

    @Data
    @Schema(name = "ProductSortItem", description = "一条排序项")
    public static class Item {

        @Schema(description = "商品 id")
        private Long id;

        @Schema(description = "排序值（数值越大越靠前）")
        private Integer sort;
    }
}
