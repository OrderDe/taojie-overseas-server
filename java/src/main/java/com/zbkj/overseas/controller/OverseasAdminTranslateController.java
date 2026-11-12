package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.TranslateRequest;
import com.zbkj.overseas.response.TranslateResponse;
import com.zbkj.overseas.service.OverseasTranslateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Name auto-translate for the admin forms. Guarded by
 * AdminTokenInterceptor like every /api/admin/** path, but with no
 * specific menu permission (requiredPerm returns null) — products,
 * categories and brands editors all use it.
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外名称翻译控制器")
public class OverseasAdminTranslateController {

    @Autowired
    private OverseasTranslateService translateService;

    @Operation(summary = "管理端：名称自动翻译（中文 -> 11 种目标语种）")
    @PostMapping("/admin/translate")
    public CommonResult<TranslateResponse> translate(@Valid @RequestBody TranslateRequest request) {
        return CommonResult.success(translateService.translate(request));
    }
}
