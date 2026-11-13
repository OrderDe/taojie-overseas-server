package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Category as the admin UI edits it: names as a map, with the live product
 * count (all products, including hidden ones — the delete guard uses it).
 */
@Data
@Schema(name = "AdminCategoryResponse", description = "海外分类（管理端）")
public class AdminCategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** Empty string means a top-level category; null in update requests preserves the parent. */
    private String parentId;

    private Integer level;

    private String pathName;

    @Schema(description = "多语言分类名 {en, zh, es, fr, pt}")
    private Map<String, String> names;

    private Integer sort;

    @Schema(description = "该分类下商品数（含下架）")
    private Integer count;
}
