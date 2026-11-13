package com.zbkj.overseas.response;

import com.zbkj.overseas.request.CustomPageSaveRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A custom page in full — the shape both the storefront page view and
 * the admin editor consume (same contract as the Node backend).
 */
@Data
@Schema(name = "CustomPageResponse", description = "自定义页面（完整）")
public class CustomPageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String slug;

    @Schema(description = "多语言页面标题 {en, zh, ...}")
    private Map<String, String> titles;

    private List<CustomPageSaveRequest.Section> sections;

    private List<String> gallery;

    @Schema(description = "是否显示")
    private Boolean enabled;

    private Integer sort;

    private String updatedAt;
}
