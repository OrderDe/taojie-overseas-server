package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.request.BrandSaveRequest;
import com.zbkj.overseas.request.CategorySaveRequest;
import com.zbkj.overseas.request.CustomPageSaveRequest;
import com.zbkj.overseas.request.ProductSaveRequest;
import com.zbkj.overseas.request.ProductSortRequest;
import com.zbkj.overseas.request.UploadRequest;
import com.zbkj.overseas.response.AdminBrandResponse;
import com.zbkj.overseas.response.AdminCategoryResponse;
import com.zbkj.overseas.response.AdminProductResponse;
import com.zbkj.overseas.response.CustomPageResponse;
import com.zbkj.overseas.service.OverseasBrandService;
import com.zbkj.overseas.service.OverseasCategoryService;
import com.zbkj.overseas.service.OverseasCustomPageService;
import com.zbkj.overseas.service.OverseasProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin controller for the management UI (static page at /admin, served from
 * classpath:/static/admin). Every path here sits under /api/admin/**, so
 * AdminTokenInterceptor guards them all — same contract as the Node backend:
 * the UI logs in with the ADMIN_TOKEN and sends it as x-admin-token.
 */
@Slf4j
@RestController
@RequestMapping("api/admin")
@Tag(name = "海外管理后台控制器")
public class OverseasAdminController {

    private static final Set<String> IMAGE_EXTS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private static final int MAX_IMAGE_BYTES = 6_000_000;
    private static final Set<String> VIDEO_EXTS = Set.of(".mp4", ".webm", ".mov");
    private static final int MAX_VIDEO_BYTES = 50_000_000;

    @Autowired
    private OverseasProductService productService;

    @Autowired
    private OverseasCategoryService categoryService;

    @Autowired
    private OverseasBrandService brandService;

    @Autowired
    private OverseasCustomPageService pageService;

    @Value("${overseas.upload-dir:./data/uploads}")
    private String uploadDir;

    // ---- products ----

    @Operation(summary = "管理端：全部商品（含下架），可按分类筛选（includeChildren 时含全部下级分类）")
    @GetMapping("/products")
    public CommonResult<List<AdminProductResponse>> products(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean includeChildren) {
        return CommonResult.success(productService.listForAdmin(categoryId, includeChildren));
    }

    @Operation(summary = "管理端：商品批量排序（一次事务更新多件商品的排序值）")
    @PostMapping("/products/sort")
    public CommonResult<Map<String, Object>> sortProducts(@RequestBody ProductSortRequest request) {
        return CommonResult.success(Map.of("ok", true, "updated", productService.updateSorts(request)));
    }

    @Operation(summary = "管理端：新建商品")
    @PostMapping("/products")
    public CommonResult<AdminProductResponse> createProduct(@RequestBody ProductSaveRequest request) {
        return CommonResult.success(productService.saveForAdmin(request, null));
    }

    @Operation(summary = "管理端：修改商品")
    @PutMapping("/products/{id}")
    public CommonResult<AdminProductResponse> updateProduct(@PathVariable Long id,
                                                            @RequestBody ProductSaveRequest request) {
        return CommonResult.success(productService.saveForAdmin(request, id));
    }

    @Operation(summary = "管理端：删除商品")
    @DeleteMapping("/products/{id}")
    public CommonResult<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        productService.deleteForAdmin(id);
        return CommonResult.success(Map.of("ok", true));
    }

    // ---- categories ----

    @Operation(summary = "管理端：全部分类")
    @GetMapping("/categories")
    public CommonResult<List<AdminCategoryResponse>> categories() {
        return CommonResult.success(categoryService.listForAdmin());
    }

    @Operation(summary = "管理端：新建分类")
    @PostMapping("/categories")
    public CommonResult<AdminCategoryResponse> createCategory(@RequestBody CategorySaveRequest request) {
        return CommonResult.success(categoryService.saveForAdmin(request, null));
    }

    @Operation(summary = "管理端：修改分类")
    @PutMapping("/categories/{id}")
    public CommonResult<AdminCategoryResponse> updateCategory(@PathVariable String id,
                                                              @RequestBody CategorySaveRequest request) {
        return CommonResult.success(categoryService.saveForAdmin(request, id));
    }

    @Operation(summary = "管理端：删除分类（下有商品时拒绝）")
    @DeleteMapping("/categories/{id}")
    public CommonResult<Map<String, Object>> deleteCategory(@PathVariable String id) {
        categoryService.deleteForAdmin(id);
        return CommonResult.success(Map.of("ok", true));
    }

    // ---- brands ----

    @Operation(summary = "管理端：全部品牌")
    @GetMapping("/brands")
    public CommonResult<List<AdminBrandResponse>> brands() {
        return CommonResult.success(brandService.listForAdmin());
    }

    @Operation(summary = "管理端：新建品牌")
    @PostMapping("/brands")
    public CommonResult<AdminBrandResponse> createBrand(@RequestBody BrandSaveRequest request) {
        return CommonResult.success(brandService.saveForAdmin(request, null));
    }

    @Operation(summary = "管理端：修改品牌")
    @PutMapping("/brands/{id}")
    public CommonResult<AdminBrandResponse> updateBrand(@PathVariable String id,
                                                        @RequestBody BrandSaveRequest request) {
        return CommonResult.success(brandService.saveForAdmin(request, id));
    }

    @Operation(summary = "管理端：删除品牌（下有商品时拒绝）")
    @DeleteMapping("/brands/{id}")
    public CommonResult<Map<String, Object>> deleteBrand(@PathVariable String id) {
        brandService.deleteForAdmin(id);
        return CommonResult.success(Map.of("ok", true));
    }

    // ---- custom pages ----

    @Operation(summary = "管理端：全部自定义页面（含隐藏）")
    @GetMapping("/pages")
    public CommonResult<List<CustomPageResponse>> pages() {
        return CommonResult.success(pageService.listForAdmin());
    }

    @Operation(summary = "管理端：新建自定义页面")
    @PostMapping("/pages")
    public CommonResult<CustomPageResponse> createPage(@RequestBody CustomPageSaveRequest request) {
        return CommonResult.success(pageService.saveForAdmin(request, null));
    }

    @Operation(summary = "管理端：修改自定义页面")
    @PutMapping("/pages/{id}")
    public CommonResult<CustomPageResponse> updatePage(@PathVariable Long id,
                                                       @RequestBody CustomPageSaveRequest request) {
        return CommonResult.success(pageService.saveForAdmin(request, id));
    }

    @Operation(summary = "管理端：删除自定义页面")
    @DeleteMapping("/pages/{id}")
    public CommonResult<Map<String, Object>> deletePage(@PathVariable Long id) {
        pageService.deleteForAdmin(id);
        return CommonResult.success(Map.of("ok", true));
    }

    // ---- image / video upload ----

    @Operation(summary = "管理端：上传商品图片或视频（base64），返回可访问地址")
    @PostMapping("/upload")
    public CommonResult<Map<String, Object>> upload(@RequestBody UploadRequest request) throws IOException {
        String filename = request.getFilename() == null ? "" : request.getFilename();
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            ext = filename.substring(dot).toLowerCase();
        }
        boolean isVideo = VIDEO_EXTS.contains(ext);
        if (!isVideo && !IMAGE_EXTS.contains(ext)) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "只允许 png/jpg/webp/gif 图片或 mp4/webm/mov 视频");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(request.getDataBase64() == null ? "" : request.getDataBase64());
        } catch (IllegalArgumentException e) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), isVideo ? "视频数据无效" : "图片数据无效");
        }
        if (isVideo) {
            if (bytes.length == 0 || bytes.length > MAX_VIDEO_BYTES) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "视频为空或超过 50 MB");
            }
        } else if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "图片为空或超过 6 MB");
        }
        String safe = Paths.get(filename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "-");
        String name = System.currentTimeMillis() + "-" + safe;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), bytes);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/").path(name).toUriString();
        return CommonResult.success(Map.of("url", url));
    }
}
