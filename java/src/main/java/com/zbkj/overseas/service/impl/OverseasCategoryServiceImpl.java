package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.dao.OverseasCategoryDao;
import com.zbkj.overseas.dao.OverseasProductDao;
import com.zbkj.overseas.model.OverseasCategory;
import com.zbkj.overseas.model.OverseasProduct;
import com.zbkj.overseas.request.CategorySaveRequest;
import com.zbkj.overseas.response.AdminCategoryResponse;
import com.zbkj.overseas.response.CategoryResponse;
import com.zbkj.overseas.service.OverseasCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    // ---- Admin (management UI) ----

    @Override
    public List<AdminCategoryResponse> listForAdmin() {
        Map<String, Long> counts = productDao.selectList(new LambdaQueryWrapper<OverseasProduct>())
                .stream().collect(Collectors.groupingBy(OverseasProduct::getCategoryId, Collectors.counting()));
        return list(new LambdaQueryWrapper<OverseasCategory>()
                .orderByAsc(OverseasCategory::getSort)
                .orderByAsc(OverseasCategory::getId))
                .stream()
                .map(c -> toAdminResponse(c, counts.getOrDefault(c.getId(), 0L).intValue()))
                .collect(Collectors.toList());
    }

    @Override
    public AdminCategoryResponse saveForAdmin(CategorySaveRequest request, String id) {
        Map<String, String> names = request.getNames();
        if (names == null || (StrUtil.isBlank(names.get("en")) && StrUtil.isBlank(names.get("zh")))) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "分类英文名/中文名至少填一个");
        }
        OverseasCategory c;
        if (id == null) {
            String newId = StrUtil.blankToDefault(StrUtil.trim(request.getId()),
                    "c" + Long.toString(System.currentTimeMillis(), 36));
            if (!newId.matches("[a-zA-Z0-9_-]+")) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "分类 ID 只能包含字母、数字、- 和 _");
            }
            if (getById(newId) != null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "分类 ID 已存在");
            }
            c = new OverseasCategory();
            c.setId(newId);
            c.setCreateTime(new Date());
        } else {
            c = getById(id);
            if (c == null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "分类不存在");
            }
        }
        c.setNames(JSONUtil.toJsonStr(names));
        c.setSort(request.getSort() == null ? 0 : request.getSort());
        c.setIsShow(true);
        c.setUpdateTime(new Date());
        saveOrUpdate(c);
        Long n = productDao.selectCount(new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getCategoryId, c.getId()));
        return toAdminResponse(c, n == null ? 0 : n.intValue());
    }

    @Override
    public void deleteForAdmin(String id) {
        Long n = productDao.selectCount(new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getCategoryId, id));
        if (n != null && n > 0) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(),
                    "该分类下还有 " + n + " 个商品，请先移动或删除商品");
        }
        removeById(id);
    }

    private AdminCategoryResponse toAdminResponse(OverseasCategory c, int count) {
        AdminCategoryResponse r = new AdminCategoryResponse();
        r.setId(c.getId());
        r.setNames(LangUtil.toStringMap(c.getNames()));
        r.setSort(c.getSort());
        r.setCount(count);
        return r;
    }
}
