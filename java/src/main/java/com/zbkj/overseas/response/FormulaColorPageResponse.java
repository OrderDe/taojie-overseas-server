package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Storefront formula search page — same shape as the Node backend
 * ({ items, total, page, pageSize }).
 */
@Data
@Schema(name = "FormulaColorPageResponse", description = "配方搜索分页结果")
public class FormulaColorPageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<FormulaColorResponse> items;

    private Long total;

    private Integer page;

    private Integer pageSize;
}
