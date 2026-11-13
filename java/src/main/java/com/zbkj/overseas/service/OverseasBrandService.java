package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasBrand;
import com.zbkj.overseas.request.BrandSaveRequest;
import com.zbkj.overseas.response.AdminBrandResponse;
import com.zbkj.overseas.response.BrandResponse;

import java.util.List;

/**
 * Overseas brand service — tjMall-service style (interface extends IService).
 * Mirrors plat's brand module (list/add/show/update/delete).
 */
public interface OverseasBrandService extends IService<OverseasBrand> {

    /** All visible brands for the storefront, names resolved for the requested language. */
    List<BrandResponse> listForFront(String lang, String categoryId);

    /** All brands for the admin UI (names as maps, counts include hidden products). */
    List<AdminBrandResponse> listForAdmin();

    /** Create (id == null) or update a brand from the admin payload. */
    AdminBrandResponse saveForAdmin(BrandSaveRequest request, String id);

    /** Delete a brand; refuses while products still reference it. */
    void deleteForAdmin(String id);

    /** Display name of a brand in a language ("" when unset/unknown). */
    String nameOf(String brandId, String lang);
}
