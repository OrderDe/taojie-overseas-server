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
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OverseasCategoryServiceImpl extends ServiceImpl<OverseasCategoryDao, OverseasCategory>
        implements OverseasCategoryService {
    @Autowired private OverseasProductDao productDao;
    private List<OverseasCategory> ordered() {
        return list(new LambdaQueryWrapper<OverseasCategory>().orderByAsc(OverseasCategory::getSort).orderByAsc(OverseasCategory::getId));
    }
    @Override public Set<String> subtreeIds(String id) { return CategoryHierarchy.subtree(ordered(), id); }
    private Map<String, Long> counts(boolean visibleOnly) {
        return productDao.selectList(new LambdaQueryWrapper<OverseasProduct>().eq(visibleOnly, OverseasProduct::getIsShow, true))
                .stream().filter(p -> p.getCategoryId() != null)
                .collect(Collectors.groupingBy(OverseasProduct::getCategoryId, Collectors.counting()));
    }
    @Override public List<CategoryResponse> listForFront(String lang) {
        List<OverseasCategory> all = ordered();
        Map<String, Long> counts = counts(true);
        Set<String> visibleIds = CategoryHierarchy.visibleIds(all);
        return all.stream().filter(c -> visibleIds.contains(c.getId())).map(c -> {
            CategoryResponse r = new CategoryResponse();
            r.setId(c.getId()); r.setParentId(CategoryHierarchy.parent(c));
            r.setLevel(CategoryHierarchy.level(all, c.getId()));
            r.setName(LangUtil.pick(c.getNames(), lang));
            r.setPathName(CategoryHierarchy.path(all, c.getId(), lang));
            r.setCount(CategoryHierarchy.count(all, c.getId(), counts));
            return r;
        }).toList();
    }
    @Override public List<AdminCategoryResponse> listForAdmin() {
        List<OverseasCategory> all = ordered(); Map<String, Long> counts = counts(false);
        return all.stream().map(c -> toAdminResponse(c, all, counts)).toList();
    }
    @Override @Transactional public AdminCategoryResponse saveForAdmin(CategorySaveRequest request, String id) {
        Map<String, String> names = request.getNames();
        if (names == null || (StrUtil.isBlank(names.get("en")) && StrUtil.isBlank(names.get("zh")))) fail("分类英文名/中文名至少填一个");
        List<OverseasCategory> all = ordered();
        OverseasCategory c;
        if (id == null) {
            id = StrUtil.blankToDefault(StrUtil.trim(request.getId()), "c" + UUID.randomUUID().toString().replace("-", ""));
            if (!id.matches("[a-zA-Z0-9_-]+") || id.length() > 64) fail("分类 ID 只能包含字母、数字、- 和 _，长度不超过 64");
            if (getById(id) != null) fail("分类 ID 已存在");
            c = new OverseasCategory().setId(id).setCreateTime(new Date());
        } else {
            c = getById(id);
            if (c == null) { fail("分类不存在"); return null; }
        }
        // Old clients that omit parentId must never detach a category during translation.
        String parentId = request.getParentId() == null ? CategoryHierarchy.parent(c) : StrUtil.trim(request.getParentId());
        try { CategoryHierarchy.validateMove(all, id, parentId); }
        catch (IllegalArgumentException e) { fail(e.getMessage()); }
        c.setParentId(parentId); c.setNames(JSONUtil.toJsonStr(names));
        c.setSort(request.getSort() == null ? 0 : request.getSort()); c.setIsShow(true); c.setUpdateTime(new Date());
        saveOrUpdate(c);
        List<OverseasCategory> updated = ordered();
        return toAdminResponse(c, updated, counts(false));
    }
    @Override @Transactional public void deleteForAdmin(String id) {
        if (count(new LambdaQueryWrapper<OverseasCategory>().eq(OverseasCategory::getParentId, id)) > 0) fail("该分类还有下级分类，请先移动或删除下级分类");
        Long n = productDao.selectCount(new LambdaQueryWrapper<OverseasProduct>().eq(OverseasProduct::getCategoryId, id));
        if (n != null && n > 0) fail("该分类下还有 " + n + " 个商品，请先移动或删除商品");
        removeById(id);
    }
    private AdminCategoryResponse toAdminResponse(OverseasCategory c, List<OverseasCategory> all, Map<String, Long> counts) {
        AdminCategoryResponse r = new AdminCategoryResponse();
        r.setId(c.getId()); r.setParentId(CategoryHierarchy.parent(c));
        r.setLevel(CategoryHierarchy.level(all, c.getId()));
        r.setPathName(CategoryHierarchy.path(all, c.getId(), "zh"));
        r.setNames(LangUtil.toStringMap(c.getNames())); r.setSort(c.getSort());
        r.setCount(CategoryHierarchy.count(all, c.getId(), counts));
        return r;
    }
    private void fail(String message) { throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), message); }
}
