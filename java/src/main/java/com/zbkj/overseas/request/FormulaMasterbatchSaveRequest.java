package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 色母 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaMasterbatchSaveRequest", description = "色母保存（管理端）")
public class FormulaMasterbatchSaveRequest {

    private String model;

    private String name;

    private String engName;

    private String type;

    private String mark;

    private Boolean enable;
}
