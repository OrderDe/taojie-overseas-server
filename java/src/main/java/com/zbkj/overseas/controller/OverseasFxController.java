package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.response.FxRatesResponse;
import com.zbkj.overseas.service.OverseasFxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public FX rates for the storefront currency switcher. Shares the
 * OverseasFxService fetch + 12h cache with the admin price calculator
 * (OverseasAdminFxController) — same response shape, no login required
 * (AdminTokenInterceptor only guards /api/admin/**).
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外汇率控制器（公开）")
public class OverseasFxController {

    @Autowired
    private OverseasFxService fxService;

    @PermitAll
    @Operation(summary = "公开：汇率表（前台币种切换，12 小时缓存）")
    @GetMapping("/fx/rates")
    public CommonResult<FxRatesResponse> rates(@RequestParam(value = "base", defaultValue = "CNY") String base) {
        String code = base == null ? "" : base.trim().toUpperCase();
        if (!code.matches("[A-Z]{3}")) {
            throw new OverseasException(400, "base must be a 3-letter currency code");
        }
        return CommonResult.success(fxService.rates(code));
    }
}
