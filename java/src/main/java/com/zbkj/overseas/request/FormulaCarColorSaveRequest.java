package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车辆颜色 create/update (admin, perm formula). Null fields are left
 * untouched on update, same as the Node backend.
 */
@Data
@Schema(name = "FormulaCarColorSaveRequest", description = "车辆颜色保存（管理端）")
public class FormulaCarColorSaveRequest {

    private Long brandId;

    private Long modelId;

    private String carColor;

    private String factoryColor;

    private String colorNum;

    private String cardNum;

    private String carImage;

    private Boolean enable;
}
