package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 国家 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaCarCountrySaveRequest", description = "国家保存（管理端）")
public class FormulaCarCountrySaveRequest {

    private String name;

    private String nameEn;

    private String language;

    private Integer code;

    private Integer state;

    private Boolean enable;

    private Integer sortRank;
}
