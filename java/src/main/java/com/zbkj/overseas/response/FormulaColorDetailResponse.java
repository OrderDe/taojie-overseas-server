package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Storefront colour detail: the car-colour row plus every colour number
 * linked through the relation table, each with LAB + formula lines.
 */
@Data
@Schema(name = "FormulaColorDetailResponse", description = "配方颜色详情")
public class FormulaColorDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String brandId;

    private String brandName;

    private String modelId;

    private String modelName;

    private String carColor;

    private String factoryColor;

    private String colorNum;

    private String cardNum;

    private String carImage;

    @Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss")
    private String updateTime;

    private List<FormulaNumberResponse> numbers;
}
