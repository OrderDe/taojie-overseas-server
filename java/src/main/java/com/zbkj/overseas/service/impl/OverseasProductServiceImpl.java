package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.page.CommonPage;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasProductDao;
import com.zbkj.overseas.model.OverseasProduct;
import com.zbkj.overseas.request.ProductSaveRequest;
import com.zbkj.overseas.request.ProductSearchRequest;
import com.zbkj.overseas.response.AdminProductResponse;
import com.zbkj.overseas.response.ProductResponse;
import com.zbkj.overseas.service.OverseasProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Overseas product service implementation.
 * Keyword search runs in memory over the multi-language names: the overseas
 * catalogue is small (synced showcase data), so no search engine is needed —
 * unlike tjmall, which splits search into its own tjMall-search module.
 */
@Service
public class OverseasProductServiceImpl extends ServiceImpl<OverseasProductDao, OverseasProduct>
        implements OverseasProductService {

    @Override
    public CommonPage<ProductResponse> pageForFront(ProductSearchRequest request) {
        String lang = LangUtil.normLang(request.getLang());
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int limit = request.getLimit() == null || request.getLimit() < 1 ? 20 : Math.min(request.getLimit(), 100);

        LambdaQueryWrapper<OverseasProduct> wrapper = new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getIsShow, true)
                .eq(request.getCategoryId() != null && !request.getCategoryId().isBlank(),
                        OverseasProduct::getCategoryId, request.getCategoryId())
                .orderByAsc(OverseasProduct::getSort)
                .orderByDesc(OverseasProduct::getSales);
        List<OverseasProduct> all = list(wrapper);

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String kw = request.getKeyword().trim().toLowerCase();
            all = all.stream().filter(p ->
                    LangUtil.pick(p.getNames(), "en").toLowerCase().contains(kw)
                            || LangUtil.pick(p.getNames(), lang).toLowerCase().contains(kw)
                            || (p.getTag() != null && p.getTag().toLowerCase().contains(kw))
            ).collect(Collectors.toList());
        }

        long total = all.size();
        List<ProductResponse> slice = all.stream()
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .map(p -> toResponse(p, lang))
                .collect(Collectors.toList());
        return CommonPage.restPage(slice, page, limit, total);
    }

    @Override
    public ProductResponse getForFront(Long id, String lang) {
        OverseasProduct product = getOne(new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getId, id)
                .eq(OverseasProduct::getIsShow, true));
        if (product == null) {
            throw new OverseasException(OverseasResultCode.PRODUCT_NOT_FOUND);
        }
        return toResponse(product, LangUtil.normLang(lang));
    }

    @Override
    public List<String> hotKeywords(String lang) {
        List<OverseasProduct> top = list(new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getIsShow, true)
                .orderByDesc(OverseasProduct::getSales)
                .last("LIMIT 30"));
        Set<String> words = new LinkedHashSet<>();
        for (OverseasProduct p : top) {
            if (p.getTag() != null && !p.getTag().isBlank()) {
                words.add(p.getTag().trim());
            }
            String first = LangUtil.pick(p.getNames(), LangUtil.normLang(lang)).split("[\\s—-]+")[0];
            if (!first.isBlank()) {
                words.add(first);
            }
            if (words.size() >= 8) {
                break;
            }
        }
        return List.copyOf(words);
    }

    @Override
    public ProductResponse toResponse(OverseasProduct p, String lang) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setCategoryId(p.getCategoryId());
        r.setName(LangUtil.pick(p.getNames(), lang));
        r.setPrice(p.getPrice());
        r.setOtPrice(p.getOtPrice());
        r.setSales(p.getSales());
        r.setUnit(p.getUnit());
        r.setMoq(p.getMoq());
        r.setImage(p.getImage());
        r.setTag(p.getTag());
        r.setDescription(LangUtil.pick(p.getBlurb(), lang));
        r.setSpecs(LangUtil.toStringMap(p.getSpecs()));
        return r;
    }

    // ---- Admin (management UI) ----

    @Override
    public List<AdminProductResponse> listForAdmin() {
        return list(new LambdaQueryWrapper<OverseasProduct>()
                .orderByAsc(OverseasProduct::getSort)
                .orderByAsc(OverseasProduct::getId))
                .stream().map(this::toAdminResponse).collect(Collectors.toList());
    }

    @Override
    public AdminProductResponse saveForAdmin(ProductSaveRequest request, Long id) {
        Map<String, String> names = request.getNames();
        if (names == null || (StrUtil.isBlank(names.get("en")) && StrUtil.isBlank(names.get("zh")))) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "商品英文名/中文名至少填一个");
        }
        OverseasProduct p;
        if (id == null) {
            p = new OverseasProduct();
            p.setCreateTime(new Date());
            // source_id has a unique key: fall back to a generated manual id.
            p.setSourceId(StrUtil.blankToDefault(StrUtil.trim(request.getSourceId()),
                    "manual-" + System.currentTimeMillis()));
        } else {
            p = getById(id);
            if (p == null) {
                throw new OverseasException(OverseasResultCode.PRODUCT_NOT_FOUND);
            }
            if (StrUtil.isNotBlank(request.getSourceId())) {
                p.setSourceId(StrUtil.trim(request.getSourceId()));
            }
        }
        p.setCategoryId(StrUtil.blankToDefault(request.getCategoryId(), ""));
        p.setNames(JSONUtil.toJsonStr(names));
        p.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        p.setOtPrice(request.getOtPrice() == null ? BigDecimal.ZERO : request.getOtPrice());
        p.setSales(request.getSales() == null ? 0 : request.getSales());
        p.setUnit(StrUtil.blankToDefault(request.getUnit(), "pc"));
        p.setMoq(request.getMoq() == null ? 0 : request.getMoq());
        p.setImage(StrUtil.blankToDefault(request.getImage(), ""));
        p.setTag(StrUtil.blankToDefault(request.getTag(), ""));
        p.setBlurb(StrUtil.blankToDefault(request.getBlurb(), ""));
        p.setSpecs(request.getSpecs() == null ? "{}" : JSONUtil.toJsonStr(request.getSpecs()));
        p.setSort(request.getSort() == null ? 0 : request.getSort());
        p.setIsShow(!Boolean.FALSE.equals(request.getActive()));
        p.setUpdateTime(new Date());
        saveOrUpdate(p);
        return toAdminResponse(p);
    }

    @Override
    public void deleteForAdmin(Long id) {
        removeById(id);
    }

    @Override
    public AdminProductResponse toAdminResponse(OverseasProduct p) {
        AdminProductResponse r = new AdminProductResponse();
        r.setId(p.getId());
        r.setSourceId(p.getSourceId());
        r.setCategoryId(p.getCategoryId());
        r.setNames(LangUtil.toStringMap(p.getNames()));
        r.setPrice(p.getPrice());
        r.setOtPrice(p.getOtPrice());
        r.setSales(p.getSales());
        r.setUnit(p.getUnit());
        r.setMoq(p.getMoq());
        r.setImage(p.getImage());
        r.setTag(p.getTag());
        r.setBlurb(LangUtil.pick(p.getBlurb(), "en"));
        r.setSpecs(parseSpecs(p.getSpecs()));
        r.setSort(p.getSort());
        r.setActive(!Boolean.FALSE.equals(p.getIsShow()));
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSpecs(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JSONUtil.toBean(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
