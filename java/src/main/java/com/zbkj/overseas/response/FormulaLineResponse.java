package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * One toner line of a colour number's formula (dose shares are computed
 * by the frontend from dose / total dose).
 */
@Data
@Schema(name = "FormulaLineResponse", description = "配方行（色母剂量）")
public class FormulaLineResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    @Schema(description = "色母型号")
    private String model;

    @Schema(description = "色母名称")
    private String name;

    @Schema(description = "剂量")
    private Double dose;

    @Schema(description = "顺序")
    private Integer colorRank;
}
