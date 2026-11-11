package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * Brand create/update (admin). id is only used on create; when blank the
 * service generates one. On update the path id wins and this field is ignored.
 */
@Data
@Schema(name = "BrandSaveRequest", description = "海外品牌保存（管理端）")
public class BrandSaveRequest {

    @Schema(description = "品牌 id（仅新建时用，可留空自动生成；只允许字母、数字、- 和 _）")
    private String id;

    @Schema(description = "多语言品牌名 {en, zh, es, fr, pt}，en/zh 至少填一个")
    private Map<String, String> names;

    @Schema(description = "品牌 logo 地址")
    private String logo;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    @Schema(description = "是否启用")
    private Boolean active;
}
