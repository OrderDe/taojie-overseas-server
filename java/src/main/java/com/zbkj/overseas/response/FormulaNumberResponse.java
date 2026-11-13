package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * A colour number with its LAB values and formula lines, as nested in
 * the storefront colour detail.
 */
@Data
@Schema(name = "FormulaNumberResponse", description = "色号（含 LAB 与配方行）")
public class FormulaNumberResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    @Schema(description = "色号")
    private String name;

    private String colorName;

    private Double labL;

    private Double labA;

    private Double labB;

    private List<FormulaLineResponse> formula;
}
