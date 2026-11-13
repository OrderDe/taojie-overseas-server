package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.common.page.CommonPage;
import com.zbkj.overseas.model.OverseasProduct;
import com.zbkj.overseas.request.ProductSaveRequest;
import com.zbkj.overseas.request.ProductSortRequest;
import com.zbkj.overseas.request.ProductSearchRequest;
import com.zbkj.overseas.response.AdminProductResponse;
import com.zbkj.overseas.response.ProductResponse;

import java.util.List;

/**
 * Overseas product service.
 */
public interface OverseasProductService extends IService<OverseasProduct> {

    /** Paged storefront list (keyword search runs over en + requested-language names). */
    CommonPage<ProductResponse> pageForFront(ProductSearchRequest request);

    /** One product for the detail page; throws OverseasException when missing/hidden. */
    ProductResponse getForFront(Long id, String lang);

    /** Hot search keywords (phase 1: top-selling products' tags + names). */
    List<String> hotKeywords(String lang);

    /** Convert an entity to its storefront response for the given language. */
    ProductResponse toResponse(OverseasProduct product, String lang);

    /** All products for the admin UI (hidden ones included), sort desc / id desc. */
    List<AdminProductResponse> listForAdmin();

    /** Batch-update sort values in one transaction; returns how many rows changed. */
    int updateSorts(ProductSortRequest request);

    /** Create (id == null) or update a product from the admin payload. */
    AdminProductResponse saveForAdmin(ProductSaveRequest request, Long id);

    /** Hard delete (the admin UI confirms first). */
    void deleteForAdmin(Long id);

    /** Convert an entity to its admin response (names as a map). */
    AdminProductResponse toAdminResponse(OverseasProduct product);
}
