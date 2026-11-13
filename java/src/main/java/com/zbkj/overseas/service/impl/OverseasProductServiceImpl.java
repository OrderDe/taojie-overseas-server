package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.page.CommonPage;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasBrandDao;
import com.zbkj.overseas.dao.OverseasCategoryDao;
import com.zbkj.overseas.dao.OverseasProductDao;
import com.zbkj.overseas.model.OverseasBrand;
import com.zbkj.overseas.model.OverseasCategory;
import com.zbkj.overseas.model.OverseasProduct;
import com.zbkj.overseas.request.ProductSaveRequest;
import com.zbkj.overseas.request.ProductSortRequest;
import com.zbkj.overseas.request.ProductSearchRequest;
import com.zbkj.overseas.response.AdminProductResponse;
import com.zbkj.overseas.response.ProductResponse;
import com.zbkj.overseas.service.OverseasProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private OverseasBrandDao brandDao;

    @Autowired
    private OverseasCategoryDao categoryDao;

    @Autowired
    private com.zbkj.overseas.service.OverseasCategoryService categoryService;

    @Override
    public CommonPage<ProductResponse> pageForFront(ProductSearchRequest request) {
        String lang = LangUtil.normLang(request.getLang());
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int limit = request.getLimit() == null || request.getLimit() < 1 ? 20 : Math.min(request.getLimit(), 100);

        Set<String> categoryIds = StrUtil.isNotBlank(request.getCategoryId())
                ? categoryService.subtreeIds(request.getCategoryId()) : Set.of();
        LambdaQueryWrapper<OverseasProduct> wrapper = new LambdaQueryWrapper<OverseasProduct>()
                .eq(OverseasProduct::getIsShow, true)
                .in(!categoryIds.isEmpty(), OverseasProduct::getCategoryId, categoryIds)
                .eq(request.getBrandId() != null && !request.getBrandId().isBlank(),
                        OverseasProduct::getBrandId, request.getBrandId())
                .orderByDesc(OverseasProduct::getSort)
                .orderByDesc(OverseasProduct::getId);
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
        if (StrUtil.isNotBlank(p.getCategoryId())) {
            java.util.LinkedList<String> path = new java.util.LinkedList<>();
            String categoryId = p.getCategoryId();
            java.util.Set<String> seen = new java.util.HashSet<>();
            while (StrUtil.isNotBlank(categoryId) && seen.add(categoryId) && path.size() < 3) {
                OverseasCategory cat = categoryDao.selectById(categoryId);
                if (cat == null) break;
                path.addFirst(LangUtil.pick(cat.getNames(), lang));
                categoryId = cat.getParentId();
            }
            r.setCategory(String.join(" / ", path));
        } else {
            r.setCategory("");
        }
        r.setBrandId(StrUtil.blankToDefault(p.getBrandId(), ""));
        if (StrUtil.isNotBlank(p.getBrandId())) {
            OverseasBrand brand = brandDao.selectById(p.getBrandId());
            r.setBrand(brand == null ? "" : LangUtil.pick(brand.getNames(), lang));
        } else {
            r.setBrand("");
        }
        r.setName(LangUtil.pick(p.getNames(), lang));
        r.setPrice(p.getPrice());
        r.setOtPrice(p.getOtPrice());
        r.setNegotiable(Boolean.TRUE.equals(p.getNegotiable()));
        r.setSales(p.getSales());
        Map<String, Object> content = LangUtil.localizedContent(p.getLocalizedContent(), lang);
        r.setUnit(String.valueOf(content.getOrDefault("unit", p.getUnit() == null ? "" : p.getUnit())));
        r.setMoq(p.getMoq());
        r.setImage(p.getImage());
        r.setVideo(p.getVideo() == null ? "" : p.getVideo());
        r.setTag(String.valueOf(content.getOrDefault("tag", p.getTag() == null ? "" : p.getTag())));
        r.setDescription(LangUtil.pick(p.getBlurb(), lang));
        r.setSpecs(content.get("specs") instanceof Map
                ? LangUtil.toStringMap(JSONUtil.toJsonStr(content.get("specs")))
                : LangUtil.toStringMap(p.getSpecs()));
        return r;
    }

    // ---- Admin (management UI) ----

    @Override
    public List<AdminProductResponse> listForAdmin() {
        return list(new LambdaQueryWrapper<OverseasProduct>()
                .orderByDesc(OverseasProduct::getSort)
                .orderByDesc(OverseasProduct::getId))
                .stream().map(this::toAdminResponse).collect(Collectors.toList());
    }

    // Batch sort save: one transaction for the whole batch, so a
    // failure never leaves a half-ordered catalogue. Invalid ids are
    // skipped; only rows that actually changed are counted.
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSorts(ProductSortRequest request) {
        if (request == null || request.getItems() == null) {
            return 0;
        }
        int updated = 0;
        for (ProductSortRequest.Item item : request.getItems()) {
            if (item == null || item.getId() == null || item.getId() <= 0) {
                continue;
            }
            boolean ok = update(new LambdaUpdateWrapper<OverseasProduct>()
                    .eq(OverseasProduct::getId, item.getId())
                    .set(OverseasProduct::getSort, item.getSort() == null ? 0 : item.getSort()));
            if (ok) {
                updated++;
            }
        }
        return updated;
    }

    @Override
    public AdminProductResponse saveForAdmin(ProductSaveRequest request, Long id) {
        Map<String, String> names = request.getNames();
        if (names == null || (StrUtil.isBlank(names.get("en")) && StrUtil.isBlank(names.get("zh")))) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "商品英文名/中文名至少填一个");
        }
        String brandId = StrUtil.blankToDefault(StrUtil.trim(request.getBrandId()), "");
        if (!brandId.isEmpty() && brandDao.selectById(brandId) == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "品牌不存在");
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
        // Clear derived translations when their source fields change.
        if (!java.util.Objects.equals(p.getUnit(), StrUtil.blankToDefault(request.getUnit(), "pc"))
                || !java.util.Objects.equals(p.getTag(), StrUtil.blankToDefault(request.getTag(), ""))
                || !parseSpecs(p.getSpecs()).equals(request.getSpecs() == null ? Map.of() : request.getSpecs())) {
            p.setLocalizedContent("{}");
        }
        if (StrUtil.isNotBlank(request.getCategoryId()) && categoryDao.selectById(request.getCategoryId()) == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "分类不存在");
        }
        p.setCategoryId(StrUtil.blankToDefault(request.getCategoryId(), ""));
        p.setBrandId(brandId);
        p.setNames(JSONUtil.toJsonStr(names));
        p.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        p.setOtPrice(request.getOtPrice() == null ? BigDecimal.ZERO : request.getOtPrice());
        p.setNegotiable(Boolean.TRUE.equals(request.getNegotiable()));
        p.setSales(request.getSales() == null ? 0 : request.getSales());
        p.setUnit(StrUtil.blankToDefault(request.getUnit(), "pc"));
        p.setMoq(request.getMoq() == null ? 0 : request.getMoq());
        p.setImage(StrUtil.blankToDefault(request.getImage(), ""));
        p.setVideo(StrUtil.blankToDefault(request.getVideo(), ""));
        p.setTag(StrUtil.blankToDefault(request.getTag(), ""));
        p.setBlurb(LangUtil.withAdminBlurb(p.getBlurb(), request.getBlurb()));
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
        r.setBrandId(StrUtil.blankToDefault(p.getBrandId(), ""));
        r.setNames(LangUtil.toStringMap(p.getNames()));
        r.setPrice(p.getPrice());
        r.setOtPrice(p.getOtPrice());
        r.setNegotiable(Boolean.TRUE.equals(p.getNegotiable()));
        r.setSales(p.getSales());
        r.setUnit(p.getUnit());
        r.setMoq(p.getMoq());
        r.setImage(p.getImage());
        r.setVideo(p.getVideo() == null ? "" : p.getVideo());
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
