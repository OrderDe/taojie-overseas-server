package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * One toner line in the whole-set replacement of a colour number's
 * formula (admin, perm formula).
 */
@Data
@Schema(name = "FormulaLineSaveRequest", description = "配方行保存（管理端）")
public class FormulaLineSaveRequest {

    @Schema(description = "色母型号")
    private String model;

    @Schema(description = "色母名称")
    private String name;

    @Schema(description = "剂量")
    private Double dose;

    @Schema(description = "顺序，缺省按列表位置")
    private Integer colorRank;
}
