package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.model.OverseasRfq;
import com.zbkj.overseas.request.SiteConfigRequest;
import com.zbkj.overseas.service.OverseasRfqService;
import com.zbkj.overseas.service.OverseasSiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Site-config + admin controller.
 * GET /api/site-config is public (the storefront reads it at boot, like
 * pc-config.json today); /api/admin/** is guarded by AdminTokenInterceptor.
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外站点配置控制器")
public class OverseasSiteConfigController {

    @Autowired
    private OverseasSiteConfigService siteConfigService;

    @Autowired
    private OverseasRfqService rfqService;

    @PermitAll
    @Operation(summary = "读取站点配置")
    @GetMapping("/site-config")
    public CommonResult<Map<String, Object>> getSiteConfig() {
        return CommonResult.success(siteConfigService.getConfigMap());
    }

    @Operation(summary = "管理端：最新询价列表")
    @GetMapping("/admin/rfq")
    public CommonResult<List<OverseasRfq>> adminRfqList() {
        return CommonResult.success(rfqService.listLatest(200));
    }

    @Operation(summary = "管理端：保存站点配置")
    @PutMapping("/admin/site-config")
    public CommonResult<Map<String, Object>> adminSaveSiteConfig(@RequestBody SiteConfigRequest request) {
        return CommonResult.success(siteConfigService.updateConfig(request));
    }
}
