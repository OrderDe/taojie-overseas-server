package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.page.CommonPage;
import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.ProductSearchRequest;
import com.zbkj.overseas.response.BrandResponse;
import com.zbkj.overseas.response.CategoryResponse;
import com.zbkj.overseas.response.ProductResponse;
import com.zbkj.overseas.service.OverseasBrandService;
import com.zbkj.overseas.service.OverseasCategoryService;
import com.zbkj.overseas.service.OverseasProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Storefront product controller — public, no login.
 * Style mirrors tjmall's front ProductController / ProductCategoryController
 * (@RestController + CommonResult + swagger @Tag/@Operation + @PermitAll),
 * but the route surface is the one the overseas PC frontend already consumes,
 * so this service replaces the interim Node service without frontend changes.
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "海外商品控制器")
public class OverseasProductController {

    @Autowired
    private OverseasProductService productService;

    @Autowired
    private OverseasCategoryService categoryService;

    @Autowired
    private OverseasBrandService brandService;

    @PermitAll
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public CommonResult<String> health() {
        return CommonResult.success("ok");
    }

    @PermitAll
    @Operation(summary = "获取海外商品分类（含在架商品数）")
    @GetMapping("/categories")
    public CommonResult<List<CategoryResponse>> categories(
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        return CommonResult.success(categoryService.listForFront(lang));
    }

    @PermitAll
    @Operation(summary = "获取海外品牌列表（仅启用的）")
    @GetMapping("/brands")
    public CommonResult<List<BrandResponse>> brands(
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        return CommonResult.success(brandService.listForFront(lang));
    }

    @PermitAll
    @Operation(summary = "海外商品列表（分页/分类/关键词）")
    @GetMapping("/products")
    public CommonResult<CommonPage<ProductResponse>> products(ProductSearchRequest request) {
        return CommonResult.success(productService.pageForFront(request));
    }

    @PermitAll
    @Operation(summary = "海外商品详情")
    @GetMapping("/products/{id}")
    public CommonResult<ProductResponse> productDetail(
            @PathVariable("id") Long id,
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        return CommonResult.success(productService.getForFront(id, lang));
    }

    @PermitAll
    @Operation(summary = "搜索热词")
    @GetMapping("/keywords")
    public CommonResult<List<String>> keywords(
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        return CommonResult.success(productService.hotKeywords(lang));
    }
}
