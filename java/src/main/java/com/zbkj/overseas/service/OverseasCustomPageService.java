package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasCustomPage;
import com.zbkj.overseas.request.CustomPageSaveRequest;
import com.zbkj.overseas.response.CustomPageResponse;
import com.zbkj.overseas.response.CustomPageSummaryResponse;

import java.util.List;

/**
 * Custom page service — admin-authored standalone pages the storefront
 * nav can point at. Mirrors the Node backend's /api/pages contract.
 */
public interface OverseasCustomPageService extends IService<OverseasCustomPage> {

    /** Enabled pages only (slug + titles), ordered for the storefront. */
    List<CustomPageSummaryResponse> listForFront();

    /** One enabled page in full; throws PAGE_NOT_FOUND when hidden/missing. */
    CustomPageResponse getForFront(String slug);

    /** All pages for the admin UI, including hidden ones. */
    List<CustomPageResponse> listForAdmin();

    /** Create (id == null) or update a page from the admin payload. */
    CustomPageResponse saveForAdmin(CustomPageSaveRequest request, Long id);

    /** Delete a page. */
    void deleteForAdmin(Long id);
}
