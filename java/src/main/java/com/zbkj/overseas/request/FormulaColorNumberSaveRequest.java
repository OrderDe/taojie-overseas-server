package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 色号 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaColorNumberSaveRequest", description = "色号保存（管理端）")
public class FormulaColorNumberSaveRequest {

    private String name;

    private String colorName;

    private Double labL;

    private Double labA;

    private Double labB;

    private Boolean enable;
}
