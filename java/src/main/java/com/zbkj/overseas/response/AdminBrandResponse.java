package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Brand as the admin UI edits it: names as a map, logo, visibility, plus
 * the live product count referencing the brand (the delete guard uses it).
 */
@Data
@Schema(name = "AdminBrandResponse", description = "海外品牌（管理端）")
public class AdminBrandResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    @Schema(description = "多语言品牌名 {en, zh, es, fr, pt}")
    private Map<String, String> names;

    private String logo;

    private Integer sort;

    @Schema(description = "是否启用")
    private Boolean active;

    @Schema(description = "该品牌下商品数")
    private Integer count;
}
