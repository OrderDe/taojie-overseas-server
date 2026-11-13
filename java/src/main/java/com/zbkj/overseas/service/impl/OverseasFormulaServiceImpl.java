package com.zbkj.overseas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasCarBrandDao;
import com.zbkj.overseas.dao.OverseasCarColorDao;
import com.zbkj.overseas.dao.OverseasCarModelDao;
import com.zbkj.overseas.dao.OverseasColorFormulaDao;
import com.zbkj.overseas.dao.OverseasColorNumberDao;
import com.zbkj.overseas.dao.OverseasColorNumberRelationDao;
import com.zbkj.overseas.model.OverseasCarBrand;
import com.zbkj.overseas.model.OverseasCarColor;
import com.zbkj.overseas.model.OverseasCarModel;
import com.zbkj.overseas.model.OverseasColorFormula;
import com.zbkj.overseas.model.OverseasColorNumber;
import com.zbkj.overseas.model.OverseasColorNumberRelation;
import com.zbkj.overseas.response.FormulaBrandResponse;
import com.zbkj.overseas.response.FormulaColorDetailResponse;
import com.zbkj.overseas.response.FormulaColorPageResponse;
import com.zbkj.overseas.response.FormulaColorResponse;
import com.zbkj.overseas.response.FormulaLineResponse;
import com.zbkj.overseas.response.FormulaModelResponse;
import com.zbkj.overseas.response.FormulaNumberResponse;
import com.zbkj.overseas.service.OverseasFormulaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Storefront paint-formula library implementation. Mirrors the Node
 * backend's /api/formula/* endpoints: only enabled, non-deleted rows
 * are visible, and every id is serialised as a string (snowflake ids).
 */
@Slf4j
@Service
public class OverseasFormulaServiceImpl implements OverseasFormulaService {

    @Autowired
    private OverseasCarBrandDao carBrandDao;

    @Autowired
    private OverseasCarModelDao carModelDao;

    @Autowired
    private OverseasCarColorDao carColorDao;

    @Autowired
    private OverseasColorNumberDao colorNumberDao;

    @Autowired
    private OverseasColorFormulaDao colorFormulaDao;

    @Autowired
    private OverseasColorNumberRelationDao relationDao;

    @Override
    public List<FormulaBrandResponse> brands() {
        List<OverseasCarBrand> rows = carBrandDao.selectList(new LambdaQueryWrapper<OverseasCarBrand>()
                .eq(OverseasCarBrand::getEnable, "1")
                .eq(OverseasCarBrand::getIsDeleted, "0")
                .orderByAsc(OverseasCarBrand::getLetterRank)
                .orderByAsc(OverseasCarBrand::getId));
        return rows.stream().map(b -> {
            FormulaBrandResponse r = new FormulaBrandResponse();
            r.setId(String.valueOf(b.getId()));
            r.setName(b.getName() == null ? "" : b.getName());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public List<FormulaModelResponse> models(Long brandId) {
        LambdaQueryWrapper<OverseasCarModel> w = new LambdaQueryWrapper<OverseasCarModel>()
                .eq(OverseasCarModel::getEnable, "1")
                .eq(OverseasCarModel::getIsDeleted, "0")
                .orderByAsc(OverseasCarModel::getSortRank)
                .orderByAsc(OverseasCarModel::getId);
        if (brandId != null) {
            w.eq(OverseasCarModel::getBrandId, brandId);
        }
        return carModelDao.selectList(w).stream().map(m -> {
            FormulaModelResponse r = new FormulaModelResponse();
            r.setId(String.valueOf(m.getId()));
            r.setBrandId(m.getBrandId() == null ? "" : String.valueOf(m.getBrandId()));
            r.setModel(m.getModel() == null ? "" : m.getModel());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public FormulaColorPageResponse search(Long brandId, Long modelId, String keyword, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.min(100, Math.max(1, pageSize));
        LambdaQueryWrapper<OverseasCarColor> w = new LambdaQueryWrapper<OverseasCarColor>()
                .eq(OverseasCarColor::getEnable, "1")
                .eq(OverseasCarColor::getIsDeleted, "0");
        if (brandId != null) {
            w.eq(OverseasCarColor::getBrandId, brandId);
        }
        if (modelId != null) {
            w.eq(OverseasCarColor::getModelId, modelId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(q -> q.like(OverseasCarColor::getCarColor, kw)
                    .or().like(OverseasCarColor::getColorNum, kw)
                    .or().like(OverseasCarColor::getFactoryColor, kw)
                    .or().like(OverseasCarColor::getCardNum, kw));
        }
        w.orderByDesc(OverseasCarColor::getUpdateTime).orderByDesc(OverseasCarColor::getId);
        Page<OverseasCarColor> result = carColorDao.selectPage(new Page<>(p, size), w);
        Map<Long, String> brandNames = brandNamesOf(result.getRecords());
        Map<Long, String> modelNames = modelNamesOf(result.getRecords());
        FormulaColorPageResponse resp = new FormulaColorPageResponse();
        resp.setItems(result.getRecords().stream()
                .map(c -> toColorResponse(c, brandNames, modelNames)).collect(Collectors.toList()));
        resp.setTotal(result.getTotal());
        resp.setPage(p);
        resp.setPageSize(size);
        return resp;
    }

    @Override
    public FormulaColorDetailResponse detail(Long id) {
        OverseasCarColor color = carColorDao.selectById(id);
        if (color == null || "1".equals(color.getIsDeleted())) {
            throw new OverseasException(OverseasResultCode.FORMULA_NOT_FOUND);
        }
        Map<Long, String> brandNames = brandNamesOf(List.of(color));
        Map<Long, String> modelNames = modelNamesOf(List.of(color));
        FormulaColorDetailResponse resp = new FormulaColorDetailResponse();
        resp.setId(String.valueOf(color.getId()));
        resp.setBrandId(String.valueOf(color.getBrandId()));
        resp.setBrandName(brandNames.getOrDefault(color.getBrandId(), ""));
        resp.setModelId(String.valueOf(color.getModelId()));
        resp.setModelName(modelNames.getOrDefault(color.getModelId(), ""));
        resp.setCarColor(nz(color.getCarColor()));
        resp.setFactoryColor(nz(color.getFactoryColor()));
        resp.setColorNum(nz(color.getColorNum()));
        resp.setCardNum(nz(color.getCardNum()));
        resp.setCarImage(nz(color.getCarImage()));
        resp.setUpdateTime(FormulaRowUtil.fmt(color.getUpdateTime()));

        // Colour numbers hang off the relation table (verified against
        // the imported data: relation.carColor = car_color.id and
        // relation.colorNumber = color_number.id).
        List<OverseasColorNumberRelation> rels = relationDao.selectList(
                new LambdaQueryWrapper<OverseasColorNumberRelation>()
                        .eq(OverseasColorNumberRelation::getCarColor, id)
                        .eq(OverseasColorNumberRelation::getEnable, "1")
                        .orderByAsc(OverseasColorNumberRelation::getColorNumber));
        List<FormulaNumberResponse> numbers = new ArrayList<>();
        for (OverseasColorNumberRelation rel : rels) {
            if (rel.getColorNumber() == null) {
                continue;
            }
            OverseasColorNumber num = colorNumberDao.selectById(rel.getColorNumber());
            if (num == null || "1".equals(num.getIsDeleted())) {
                continue;
            }
            FormulaNumberResponse nr = new FormulaNumberResponse();
            nr.setId(String.valueOf(num.getId()));
            nr.setName(nz(num.getName()));
            nr.setColorName(nz(num.getColorName()));
            nr.setLabL(num.getLabL());
            nr.setLabA(num.getLabA());
            nr.setLabB(num.getLabB());
            List<OverseasColorFormula> lines = colorFormulaDao.selectList(
                    new LambdaQueryWrapper<OverseasColorFormula>()
                            .eq(OverseasColorFormula::getColornumId, num.getId())
                            .eq(OverseasColorFormula::getIsDeleted, "0")
                            .orderByAsc(OverseasColorFormula::getColorRank)
                            .orderByAsc(OverseasColorFormula::getId));
            nr.setFormula(lines.stream().map(this::toLineResponse).collect(Collectors.toList()));
            numbers.add(nr);
        }
        resp.setNumbers(numbers);
        return resp;
    }

    private FormulaLineResponse toLineResponse(OverseasColorFormula line) {
        FormulaLineResponse r = new FormulaLineResponse();
        r.setId(String.valueOf(line.getId()));
        r.setModel(nz(line.getModel()));
        r.setName(nz(line.getName()));
        r.setDose(line.getDose());
        r.setColorRank(line.getColorRank());
        return r;
    }

    private FormulaColorResponse toColorResponse(OverseasCarColor c, Map<Long, String> brandNames, Map<Long, String> modelNames) {
        FormulaColorResponse r = new FormulaColorResponse();
        r.setId(String.valueOf(c.getId()));
        r.setBrandId(String.valueOf(c.getBrandId()));
        r.setBrandName(brandNames.getOrDefault(c.getBrandId(), ""));
        r.setModelId(String.valueOf(c.getModelId()));
        r.setModelName(modelNames.getOrDefault(c.getModelId(), ""));
        r.setCarColor(nz(c.getCarColor()));
        r.setFactoryColor(nz(c.getFactoryColor()));
        r.setColorNum(nz(c.getColorNum()));
        r.setCardNum(nz(c.getCardNum()));
        r.setCarImage(nz(c.getCarImage()));
        r.setUpdateTime(FormulaRowUtil.fmt(c.getUpdateTime()));
        return r;
    }

    private Map<Long, String> brandNamesOf(List<OverseasCarColor> colors) {
        Map<Long, String> names = new HashMap<>();
        List<Long> ids = colors.stream().map(OverseasCarColor::getBrandId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (!ids.isEmpty()) {
            for (OverseasCarBrand b : carBrandDao.selectBatchIds(ids)) {
                names.put(b.getId(), b.getName() == null ? "" : b.getName());
            }
        }
        return names;
    }

    private Map<Long, String> modelNamesOf(List<OverseasCarColor> colors) {
        Map<Long, String> names = new HashMap<>();
        List<Long> ids = colors.stream().map(OverseasCarColor::getModelId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (!ids.isEmpty()) {
            for (OverseasCarModel m : carModelDao.selectBatchIds(ids)) {
                names.put(m.getId(), m.getModel() == null ? "" : m.getModel());
            }
        }
        return names;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
