package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.response.FxRatesResponse;
import com.zbkj.overseas.service.OverseasFxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FX rates for the admin price calculator. Guarded by
 * AdminTokenInterceptor like every /api/admin/** path, but with no
 * specific menu permission (requiredPerm returns null) — every signed-in
 * account, sales staff included, can use the calculator.
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外汇率控制器")
public class OverseasAdminFxController {

    @Autowired
    private OverseasFxService fxService;

    @Operation(summary = "管理端：汇率表（试算工具，12 小时缓存）")
    @GetMapping("/admin/fx/rates")
    public CommonResult<FxRatesResponse> rates(@RequestParam(value = "base", defaultValue = "CNY") String base) {
        String code = base == null ? "" : base.trim().toUpperCase();
        if (!code.matches("[A-Z]{3}")) {
            throw new OverseasException(400, "base must be a 3-letter currency code");
        }
        return CommonResult.success(fxService.rates(code));
    }
}
