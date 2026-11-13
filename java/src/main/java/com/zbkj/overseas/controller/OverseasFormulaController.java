package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.response.FormulaBrandResponse;
import com.zbkj.overseas.response.FormulaColorDetailResponse;
import com.zbkj.overseas.response.FormulaColorPageResponse;
import com.zbkj.overseas.response.FormulaModelResponse;
import com.zbkj.overseas.service.OverseasFormulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Storefront paint-formula library — public, no login. Routes and JSON
 * shapes are the ones the overseas PC frontend consumes from the Node
 * backend (/api/formula/*), so this service is a drop-in replacement.
 */
@Slf4j
@RestController
@RequestMapping("api/formula")
@Tag(name = "海外配方库控制器")
public class OverseasFormulaController {

    @Autowired
    private OverseasFormulaService formulaService;

    @PermitAll
    @Operation(summary = "配方库：启用的汽车品牌（按 letter_rank）")
    @GetMapping("/brands")
    public CommonResult<List<FormulaBrandResponse>> brands() {
        return CommonResult.success(formulaService.brands());
    }

    @PermitAll
    @Operation(summary = "配方库：车型（brandId 联动，可空=全部）")
    @GetMapping("/models")
    public CommonResult<List<FormulaModelResponse>> models(
            @RequestParam(name = "brandId", required = false) Long brandId) {
        return CommonResult.success(formulaService.models(brandId));
    }

    @PermitAll
    @Operation(summary = "配方库：车辆颜色搜索（品牌/车型/关键词，分页）")
    @GetMapping("/search")
    public CommonResult<FormulaColorPageResponse> search(
            @RequestParam(name = "brandId", required = false) Long brandId,
            @RequestParam(name = "modelId", required = false) Long modelId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "15") int pageSize) {
        return CommonResult.success(formulaService.search(brandId, modelId, keyword, page, pageSize));
    }

    @PermitAll
    @Operation(summary = "配方库：颜色详情（色号 + LAB + 配方行）")
    @GetMapping("/colors/{id}")
    public CommonResult<FormulaColorDetailResponse> detail(@PathVariable("id") Long id) {
        return CommonResult.success(formulaService.detail(id));
    }
}
