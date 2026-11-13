package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 汽车品牌 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaCarBrandSaveRequest", description = "汽车品牌保存（管理端）")
public class FormulaCarBrandSaveRequest {

    private String name;

    private String nameFirst;

    private String country;

    private String url;

    private Integer letterRank;

    private Boolean enable;
}
