package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 汽车生产商 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaCarFactorySaveRequest", description = "汽车生产商保存（管理端）")
public class FormulaCarFactorySaveRequest {

    private String name;

    private Long brandId;

    private String country;

    private Integer state;

    private Boolean enable;

    private Integer sortRank;
}
