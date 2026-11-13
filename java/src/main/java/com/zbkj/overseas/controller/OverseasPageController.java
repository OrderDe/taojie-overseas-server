package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.response.CustomPageResponse;
import com.zbkj.overseas.response.CustomPageSummaryResponse;
import com.zbkj.overseas.service.OverseasCustomPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Storefront custom-page controller — public, no login. Custom pages are
 * admin-authored standalone pages; the nav can point an item at one by
 * slug. Hidden or unknown slugs fail with PAGE_NOT_FOUND, mirroring the
 * Node backend's 404 on GET /api/pages/:slug.
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外自定义页面控制器")
public class OverseasPageController {

    @Autowired
    private OverseasCustomPageService pageService;

    @PermitAll
    @Operation(summary = "自定义页面列表（仅显示中的：slug + 多语言标题）")
    @GetMapping("/pages")
    public CommonResult<List<CustomPageSummaryResponse>> pages() {
        return CommonResult.success(pageService.listForFront());
    }

    @PermitAll
    @Operation(summary = "自定义页面详情（按 slug）")
    @GetMapping("/pages/{slug}")
    public CommonResult<CustomPageResponse> pageDetail(@PathVariable("slug") String slug) {
        return CommonResult.success(pageService.getForFront(slug));
    }
}
