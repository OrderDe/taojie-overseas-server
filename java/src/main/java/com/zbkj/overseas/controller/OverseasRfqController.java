package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.RfqRequest;
import com.zbkj.overseas.service.OverseasRfqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Storefront RFQ controller — public lead capture.
 * Rate limiting lives in the service (5/min per IP); a saved lead can fire
 * the configured webhook for mail/WhatsApp notification.
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外询价控制器")
public class OverseasRfqController {

    @Autowired
    private OverseasRfqService rfqService;

    @PermitAll
    @Operation(summary = "提交询价（留资）")
    @PostMapping("/rfq")
    public CommonResult<Map<String, Object>> submitRfq(@Valid @RequestBody RfqRequest request,
                                                       HttpServletRequest httpRequest) {
        Long id = rfqService.submit(request, clientIp(httpRequest));
        return CommonResult.success(Map.of("ok", true, "id", id));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
