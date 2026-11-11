package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasCategory;
import com.zbkj.overseas.request.CategorySaveRequest;
import com.zbkj.overseas.response.AdminCategoryResponse;
import com.zbkj.overseas.response.CategoryResponse;

import java.util.List;

/**
 * Overseas category service — tjMall-service style (interface extends IService).
 */
public interface OverseasCategoryService extends IService<OverseasCategory> {

    /** All visible categories, names resolved for the requested language, with live product counts. */
    List<CategoryResponse> listForFront(String lang);

    /** All categories for the admin UI (names as maps, counts include hidden products). */
    List<AdminCategoryResponse> listForAdmin();

    /** Create (id == null) or update a category from the admin payload. */
    AdminCategoryResponse saveForAdmin(CategorySaveRequest request, String id);

    /** Delete a category; refuses while products still reference it. */
    void deleteForAdmin(String id);
}
