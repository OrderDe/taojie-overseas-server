package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车型 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaCarModelSaveRequest", description = "车型保存（管理端）")
public class FormulaCarModelSaveRequest {

    private String brand;

    private Long brandId;

    private String model;

    private Integer sortRank;

    private Boolean enable;
}
