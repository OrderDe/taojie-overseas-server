package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Category row returned to the storefront — response/ package mirrors
 * tjMall-app's front/response style.
 */
@Data
@Schema(name = "CategoryResponse", description = "海外商品分类")
public class CategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** Empty string means a top-level category; null in update requests preserves the parent. */
    private String parentId;

    private Integer level;

    private String pathName;

    @Schema(description = "按请求语言解析后的分类名")
    private String name;

    @Schema(description = "该分类下在架商品数")
    private Integer count;
}
