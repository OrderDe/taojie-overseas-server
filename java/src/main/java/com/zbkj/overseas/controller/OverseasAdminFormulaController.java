package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.FormulaLinesSaveRequest;
import com.zbkj.overseas.response.FormulaAdminPageResponse;
import com.zbkj.overseas.response.FormulaLinesResponse;
import com.zbkj.overseas.service.OverseasAdminFormulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin paint-formula library. Every path sits under /api/admin/formula
 * or /api/admin/formula-numbers, so AdminTokenInterceptor guards them
 * with the per-resource menu permission keys — same contract as the
 * Node backend. Entity keys: car-brands / car-models / car-countries /
 * car-factories / color-cards / masterbatches / car-colors /
 * color-brands. Bodies are plain JSON maps, applied by field name.
 */
@Slf4j
@RestController
@RequestMapping("api/admin")
@Tag(name = "海外配方库控制器（管理端）")
public class OverseasAdminFormulaController {

    @Autowired
    private OverseasAdminFormulaService formulaService;

    @Operation(summary = "管理端：配方字典分页列表（keyword/brandId/modelId 筛选）")
    @GetMapping("/formula/{entity}")
    public CommonResult<FormulaAdminPageResponse> list(
            @PathVariable String entity,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brandId", required = false) Long brandId,
            @RequestParam(name = "modelId", required = false) Long modelId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return CommonResult.success(formulaService.list(entity, keyword, brandId, modelId, page, pageSize));
    }

    @Operation(summary = "管理端：配方字典新增")
    @PostMapping("/formula/{entity}")
    public CommonResult<Map<String, Object>> create(
            @PathVariable String entity, @RequestBody Map<String, Object> body) {
        return CommonResult.success(formulaService.create(entity, body));
    }

    @Operation(summary = "管理端：配方字典编辑")
    @PutMapping("/formula/{entity}/{id}")
    public CommonResult<Map<String, Object>> update(
            @PathVariable String entity, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return CommonResult.success(formulaService.update(entity, id, body));
    }

    @Operation(summary = "管理端：配方字典删除（物理删）")
    @DeleteMapping("/formula/{entity}/{id}")
    public CommonResult<Boolean> delete(@PathVariable String entity, @PathVariable Long id) {
        formulaService.delete(entity, id);
        return CommonResult.success(true);
    }

    @Operation(summary = "管理端：色号分页列表")
    @GetMapping("/formula-numbers")
    public CommonResult<FormulaAdminPageResponse> listNumbers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return CommonResult.success(formulaService.listNumbers(keyword, page, pageSize));
    }

    @Operation(summary = "管理端：色号新增")
    @PostMapping("/formula-numbers")
    public CommonResult<Map<String, Object>> createNumber(@RequestBody Map<String, Object> body) {
        return CommonResult.success(formulaService.createNumber(body));
    }

    @Operation(summary = "管理端：色号编辑")
    @PutMapping("/formula-numbers/{id}")
    public CommonResult<Map<String, Object>> updateNumber(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return CommonResult.success(formulaService.updateNumber(id, body));
    }

    @Operation(summary = "管理端：色号删除（物理删）")
    @DeleteMapping("/formula-numbers/{id}")
    public CommonResult<Boolean> deleteNumber(@PathVariable Long id) {
        formulaService.deleteNumber(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "管理端：读取色号的配方行")
    @GetMapping("/formula-numbers/{id}/lines")
    public CommonResult<FormulaLinesResponse> getLines(@PathVariable Long id) {
        return CommonResult.success(formulaService.getLines(id));
    }

    @Operation(summary = "管理端：整体替换色号的配方行")
    @PutMapping("/formula-numbers/{id}/lines")
    public CommonResult<FormulaLinesResponse> replaceLines(
            @PathVariable Long id, @RequestBody FormulaLinesSaveRequest request) {
        return CommonResult.success(formulaService.replaceLines(id, request));
    }
}
