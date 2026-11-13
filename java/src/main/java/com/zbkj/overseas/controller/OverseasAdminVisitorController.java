package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.response.VisitEventResponse;
import com.zbkj.overseas.response.VisitorStatsResponse;
import com.zbkj.overseas.service.OverseasVisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin visitor statistics. Every path sits under /api/admin/visitors,
 * so AdminTokenInterceptor guards them with the "visitors" menu
 * permission — same contract as the Node backend.
 */
@Slf4j
@RestController
@RequestMapping("api/admin/visitors")
@Tag(name = "海外访客统计控制器（管理端）")
public class OverseasAdminVisitorController {

    @Autowired
    private OverseasVisitorService visitorService;

    @Operation(summary = "管理端：访客统计总览（summary/国家分布/7天趋势/最近100访客）")
    @GetMapping
    public CommonResult<VisitorStatsResponse> stats() {
        return CommonResult.success(visitorService.stats());
    }

    @Operation(summary = "管理端：单个访客的行程时间线（浏览+点击，时间倒序）")
    @GetMapping("/{token}/events")
    public CommonResult<List<VisitEventResponse>> events(
            @PathVariable String token,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return CommonResult.success(visitorService.events(token, limit));
    }
}
