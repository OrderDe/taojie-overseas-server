package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasCategory;
import com.zbkj.overseas.response.CategoryResponse;

import java.util.List;

/**
 * Overseas category service — tjMall-service style (interface extends IService).
 */
public interface OverseasCategoryService extends IService<OverseasCategory> {

    /** All visible categories, names resolved for the requested language, with live product counts. */
    List<CategoryResponse> listForFront(String lang);
}
