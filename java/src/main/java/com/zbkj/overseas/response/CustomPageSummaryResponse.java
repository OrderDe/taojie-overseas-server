package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * One row of the public custom-page list: just the handle and titles,
 * so the storefront nav can resolve a page-type item's label/target.
 */
@Data
@Schema(name = "CustomPageSummaryResponse", description = "自定义页面（列表项）")
public class CustomPageSummaryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String slug;

    @Schema(description = "多语言页面标题 {en, zh, ...}")
    private Map<String, String> titles;
}
