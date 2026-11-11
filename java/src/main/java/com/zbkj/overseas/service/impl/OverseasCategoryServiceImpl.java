package com.zbkj.overseas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.dao.OverseasCategoryDao;
import com.zbkj.overseas.dao.OverseasProductDao;
import com.zbkj.overseas.model.OverseasCategory;
import com.zbkj.overseas.model.OverseasProduct;
import com.zbkj.overseas.response.CategoryResponse;
import com.zbkj.overseas.service.OverseasCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Overseas category service implementation.
 */
@Service
public class OverseasCategoryServiceImpl extends ServiceImpl<OverseasCategoryDao, OverseasCategory>
        implements OverseasCategoryService {

    @Autowired
    private OverseasProductDao productDao;

    @Override
    public List<CategoryResponse> listForFront(String lang) {
        List<OverseasCategory> categories = list(new LambdaQueryWrapper<OverseasCategory>()
                .eq(OverseasCategory::getIsShow, true)
                .orderByAsc(OverseasCategory::getSort));
        Map<String, Long> counts = productDao.selectList(new LambdaQueryWrapper<OverseasProduct>()
                        .eq(OverseasProduct::getIsShow, true))
                .stream().collect(Collectors.groupingBy(OverseasProduct::getCategoryId, Collectors.counting()));
        return categories.stream().map(c -> {
            CategoryResponse r = new CategoryResponse();
            r.setId(c.getId());
            r.setName(LangUtil.pick(c.getNames(), lang));
            r.setCount(counts.getOrDefault(c.getId(), 0L).intValue());
            return r;
        }).collect(Collectors.toList());
    }
}
