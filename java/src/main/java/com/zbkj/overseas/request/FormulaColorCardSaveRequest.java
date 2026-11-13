package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 色卡 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaColorCardSaveRequest", description = "色卡保存（管理端）")
public class FormulaColorCardSaveRequest {

    private String name;

    private String code;

    private Boolean enable;
}
