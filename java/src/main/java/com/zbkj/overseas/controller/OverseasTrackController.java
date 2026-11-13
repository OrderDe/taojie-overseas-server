package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.TrackRequest;
import com.zbkj.overseas.service.OverseasVisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Storefront visitor-statistics beacon — public, token-based (no
 * login), same contract as the Node backend. The response wraps in
 * CommonResult here; the storefront only reads data.token when present
 * and ignores failures either way.
 */
@Slf4j
@RestController
@RequestMapping("api/track")
@Tag(name = "海外访客统计控制器（访客端）")
public class OverseasTrackController {

    @Autowired
    private OverseasVisitorService visitorService;

    @PermitAll
    @Operation(summary = "上报一次浏览/点击/心跳，返回生效的访客 token")
    @PostMapping
    public CommonResult<Map<String, Object>> track(@RequestBody(required = false) TrackRequest request,
                                                   HttpServletRequest httpRequest) {
        String token = visitorService.track(request, clientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        return CommonResult.success(Map.of("ok", true, "token", token));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
