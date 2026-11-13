package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Replace-all payload for a colour number's formula lines (admin,
 * perm formula): the stored set is swapped for {@code lines} in one go,
 * same contract as the Node backend.
 */
@Data
@Schema(name = "FormulaLinesSaveRequest", description = "色号配方行整体替换（管理端）")
public class FormulaLinesSaveRequest {

    private List<FormulaLineSaveRequest> lines;
}
