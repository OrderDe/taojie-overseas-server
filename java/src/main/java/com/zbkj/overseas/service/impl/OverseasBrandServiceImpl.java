package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.dao.OverseasBrandDao;
import com.zbkj.overseas.dao.OverseasProductDao;
import com.zbkj.overseas.model.OverseasBrand;
import com.zbkj.overseas.model.OverseasProduct;
import com.zbkj.overseas.request.BrandSaveRequest;
import com.zbkj.overseas.response.AdminBrandResponse;
import com.zbkj.overseas.response.BrandResponse;
import com.zbkj.overseas.service.OverseasBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Overseas brand service implementation.
 */
@Service
public class OverseasBrandServiceImpl extends ServiceImpl<OverseasBrandDao, OverseasBrand>
        implements OverseasBrandService {

    @Autowired
    private OverseasProductDao productDao;

    @Override
    public List<BrandResponse> listForFront(String lang) {
        return list(new LambdaQueryWrapper<OverseasBrand>()
                .eq(OverseasBrand::getIsShow, true)
                .orderByAsc(OverseasBrand::getSort)
                .orderByAsc(OverseasBrand::getId))
                .stream().map(b -> {
                    BrandResponse r = new BrandResponse();
                    r.setId(b.getId());
                    r.setName(LangUtil.pick(b.getNames(), lang));
                    r.setLogo(StrUtil.blankToDefault(b.getLogo(), ""));
                    return r;
                }).collect(Collectors.toList());
    }

    // ---- Admin (management UI) ----

    @Override
    public List<AdminBrandResponse> listForAdmin() {
        Map<String, Long> counts = productDao.selectList(new LambdaQueryWrapper<OverseasProduct>())
                .stream()
                .filter(p -> StrUtil.isNotBlank(p.getBrandId()))
                .collect(Collectors.groupingBy(OverseasProduct::getBrandId, Collectors.counting()));
        return list(new LambdaQueryWrapper<OverseasBrand>()
                .orderByAsc(OverseasBrand::getSort)
                .orderByAsc(OverseasBrand::getId))
                .stream()
                .map(b -> toAdminResponse(b, counts.getOrDefault(b.getId(), 0L).intValue()))
                .collect(Collectors.toList());
    }

    @Override
    public AdminBrandResponse saveForAdmin(BrandSaveRequest request, String id) {
        Map<String, String> names = request.getNames();
        if (names == null || (StrUtil.isBlank(names.get("en")) && StrUtil.isBlank(names.get("zh")))) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "品牌英文名/中文名至少填一个");
        }
        OverseasBrand b;
        if (id == null) {
            String newId = StrUtil.blankToDefault(StrUtil.trim(request.getId()),
                    "b" + Long.toString(System.currentTimeMillis(), 36));
            if (!newId.matches("[a-zA-Z0-9_-]+")) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "品牌 ID 只能包含字母、数字、- 和 _");
            }
            if (getById(newId) != null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "品牌 ID 已存在");
            }
            b = new OverseasBrand();
            b.setId(newId);
            b.setCreateTime(new Date());
        } else {
            b = getById(id);
            if (b == null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "品牌不存在");
            }
        }
        b.setNames(JSONUtil.toJsonStr(names));
        b.setLogo(StrUtil.blankToDefault(request.getLogo(), ""));
        b.setSort(request.getSort() == null ? 0 : request.getSort());
        b.setIsShow(!Boolean.FALSE.equals(request.getActive()));
        b.setUpdateTime(new Date());
        saveOrUpdate(b);
        Long n = productDao.selectCount(new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getBrandId, b.getId()));
        return toAdminResponse(b, n == null ? 0 : n.intValue());
    }

    @Override
    public void deleteForAdmin(String id) {
        Long n = productDao.selectCount(new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getBrandId, id));
        if (n != null && n > 0) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(),
                    "该品牌下还有 " + n + " 个商品，请先改派或删除商品");
        }
        removeById(id);
    }

    @Override
    public String nameOf(String brandId, String lang) {
        if (StrUtil.isBlank(brandId)) {
            return "";
        }
        OverseasBrand b = getById(brandId);
        return b == null ? "" : LangUtil.pick(b.getNames(), lang);
    }

    private AdminBrandResponse toAdminResponse(OverseasBrand b, int count) {
        AdminBrandResponse r = new AdminBrandResponse();
        r.setId(b.getId());
        r.setNames(LangUtil.toStringMap(b.getNames()));
        r.setLogo(StrUtil.blankToDefault(b.getLogo(), ""));
        r.setSort(b.getSort());
        r.setActive(!Boolean.FALSE.equals(b.getIsShow()));
        r.setCount(count);
        return r;
    }
}
