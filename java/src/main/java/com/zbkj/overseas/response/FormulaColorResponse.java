package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * One car-colour row in the storefront formula search results.
 * Ids are strings (snowflake ids in the source data).
 */
@Data
@Schema(name = "FormulaColorResponse", description = "配方搜索结果行")
public class FormulaColorResponse implements Serializable {

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
}
