package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 产品品牌 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaColorBrandSaveRequest", description = "产品品牌保存（管理端）")
public class FormulaColorBrandSaveRequest {

    private String name;

    private String logo;

    private Boolean enable;
}
