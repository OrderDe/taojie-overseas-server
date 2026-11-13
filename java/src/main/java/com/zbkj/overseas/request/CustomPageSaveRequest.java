package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Custom page create/update (admin). On update the service allows the
 * slug to change only when no other page already uses the new one;
 * nav items reference pages by slug.
 */
@Data
@Schema(name = "CustomPageSaveRequest", description = "自定义页面保存（管理端）")
public class CustomPageSaveRequest {

    @Schema(description = "页面路径标识：^[a-z0-9][a-z0-9-]{1,60}$，全站唯一")
    private String slug;

    @Schema(description = "多语言页面标题 {en, zh, ...}，至少填一个语言")
    private Map<String, String> titles;

    @Schema(description = "内容区块列表（最多 30 个）")
    private List<Section> sections;

    @Schema(description = "图库图片 URL 列表（最多 30 张）")
    private List<String> gallery;

    @Schema(description = "是否显示")
    private Boolean enabled;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    /** One content block of a custom page: heading + multi-paragraph body + optional image. */
    @Data
    @Schema(name = "CustomPageSection", description = "自定义页面内容区块")
    public static class Section {

        @Schema(description = "标题（多语言 {语言码: 文案}）")
        private Map<String, String> title;

        @Schema(description = "正文（多语言 {语言码: 文案}，空行分段）")
        private Map<String, String> body;

        @Schema(description = "配图 URL（可空）")
        private String image;
    }
}
