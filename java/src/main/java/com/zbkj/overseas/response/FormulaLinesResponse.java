package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * The formula lines of one colour number (admin editor read/write shape,
 * same as the Node backend: { colorNumberId, lines }).
 */
@Data
@Schema(name = "FormulaLinesResponse", description = "色号的配方行集合")
public class FormulaLinesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String colorNumberId;

    private List<FormulaLineResponse> lines;
}
