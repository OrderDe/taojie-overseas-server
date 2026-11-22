package com.zbkj.overseas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.dao.OverseasCarBrandDao;
import com.zbkj.overseas.dao.OverseasCarColorDao;
import com.zbkj.overseas.dao.OverseasCarCountryDao;
import com.zbkj.overseas.dao.OverseasCarFactoryDao;
import com.zbkj.overseas.dao.OverseasCarModelDao;
import com.zbkj.overseas.dao.OverseasColorBrandDao;
import com.zbkj.overseas.dao.OverseasColorCardDao;
import com.zbkj.overseas.dao.OverseasColorFormulaDao;
import com.zbkj.overseas.dao.OverseasColorMasterbatchDao;
import com.zbkj.overseas.dao.OverseasColorNumberDao;
import com.zbkj.overseas.model.OverseasCarBrand;
import com.zbkj.overseas.model.OverseasCarColor;
import com.zbkj.overseas.model.OverseasCarCountry;
import com.zbkj.overseas.model.OverseasCarFactory;
import com.zbkj.overseas.model.OverseasCarModel;
import com.zbkj.overseas.model.OverseasColorBrand;
import com.zbkj.overseas.model.OverseasColorCard;
import com.zbkj.overseas.model.OverseasColorFormula;
import com.zbkj.overseas.model.OverseasColorMasterbatch;
import com.zbkj.overseas.model.OverseasColorNumber;
import com.zbkj.overseas.request.FormulaLineSaveRequest;
import com.zbkj.overseas.request.FormulaLinesSaveRequest;
import com.zbkj.overseas.response.FormulaAdminPageResponse;
import com.zbkj.overseas.response.FormulaLineResponse;
import com.zbkj.overseas.response.FormulaLinesResponse;
import com.zbkj.overseas.service.OverseasAdminFormulaService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin paint-formula CRUD implementation — a config-driven engine that
 * mirrors the Node backend's FORMULA_CRUD table one-to-one (same entity
 * keys, search columns, filters, ordering and required fields), so both
 * backends expose the identical contract. Deletes are physical, same
 * as the catalogue modules and the Node backend.
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OverseasAdminFormulaServiceImpl implements OverseasAdminFormulaService {

    /** One entity's CRUD definition (the FORMULA_CRUD counterpart). */
    private static final class Spec {
        private BaseMapper dao;
        private Class<?> entityClass;
        private boolean hasDeleted;
        private boolean inputId;
        private List<String> searchCols = List.of();
        private Map<String, String> filterCols = Map.of();
        private List<String> orderCols = List.of("id");
        private boolean orderDesc;
        private List<String> required = List.of();
    }

    private final Map<String, Spec> specs = new HashMap<>();

    @Autowired
    private OverseasCarBrandDao carBrandDao;
    @Autowired
    private OverseasCarModelDao carModelDao;
    @Autowired
    private OverseasCarCountryDao carCountryDao;
    @Autowired
    private OverseasCarFactoryDao carFactoryDao;
    @Autowired
    private OverseasColorCardDao colorCardDao;
    @Autowired
    private OverseasColorMasterbatchDao masterbatchDao;
    @Autowired
    private OverseasCarColorDao carColorDao;
    @Autowired
    private OverseasColorBrandDao colorBrandDao;
    @Autowired
    private OverseasColorNumberDao colorNumberDao;
    @Autowired
    private OverseasColorFormulaDao colorFormulaDao;

    // Ordering note: every admin list below is pure id-desc (newest
    // first) by product decision — the letter_rank / sort_rank fields
    // remain editable form fields but no longer order admin lists.
    @PostConstruct
    public void initSpecs() {
        Spec s;
        s = new Spec();
        s.dao = carBrandDao; s.entityClass = OverseasCarBrand.class; s.hasDeleted = true;
        s.searchCols = List.of("name"); s.orderCols = List.of("id"); s.orderDesc = true;
        specs.put("car-brands", s);

        s = new Spec();
        s.dao = carModelDao; s.entityClass = OverseasCarModel.class; s.hasDeleted = true;
        s.searchCols = List.of("model", "brand"); s.filterCols = Map.of("brandId", "brand_id");
        s.orderCols = List.of("id"); s.orderDesc = true; s.required = List.of("model", "brandId");
        specs.put("car-models", s);

        s = new Spec();
        s.dao = carCountryDao; s.entityClass = OverseasCarCountry.class; s.hasDeleted = false;
        s.searchCols = List.of("name", "name_en"); s.orderCols = List.of("id"); s.orderDesc = true;
        // Bug #19: name/nameEn/language/code are required server-side too;
        // state/sortRank carry dialog defaults, so they are only enforced
        // by the admin form, not blocked here.
        s.required = List.of("name", "nameEn", "language", "code");
        specs.put("car-countries", s);

        s = new Spec();
        s.dao = carFactoryDao; s.entityClass = OverseasCarFactory.class; s.hasDeleted = false; s.inputId = true;
        s.searchCols = List.of("name"); s.filterCols = Map.of("brandId", "brand_id");
        s.orderCols = List.of("id"); s.orderDesc = true;
        // Bug #18: same split as countries — state/sortRank stay form-side.
        s.required = List.of("name", "brandId", "country");
        specs.put("car-factories", s);

        s = new Spec();
        s.dao = colorCardDao; s.entityClass = OverseasColorCard.class; s.hasDeleted = true; s.inputId = true;
        s.searchCols = List.of("name", "code"); s.orderCols = List.of("id"); s.orderDesc = true;
        s.required = List.of("name", "code");
        specs.put("color-cards", s);

        s = new Spec();
        s.dao = masterbatchDao; s.entityClass = OverseasColorMasterbatch.class; s.hasDeleted = true; s.inputId = true;
        s.searchCols = List.of("model", "name", "eng_name"); s.orderCols = List.of("id"); s.orderDesc = true;
        s.required = List.of("model", "name", "engName");
        specs.put("masterbatches", s);

        s = new Spec();
        s.dao = carColorDao; s.entityClass = OverseasCarColor.class; s.hasDeleted = true;
        s.searchCols = List.of("car_color", "color_num", "factory_color", "card_num");
        s.filterCols = Map.of("brandId", "brand_id", "modelId", "model_id");
        s.orderCols = List.of("id"); s.orderDesc = true;
        s.required = List.of("brandId", "modelId");
        specs.put("car-colors", s);

        s = new Spec();
        s.dao = colorBrandDao; s.entityClass = OverseasColorBrand.class; s.hasDeleted = true;
        s.searchCols = List.of("name"); s.orderCols = List.of("id"); s.orderDesc = true;
        s.required = List.of("name", "logo");
        specs.put("color-brands", s);

        s = new Spec();
        s.dao = colorNumberDao; s.entityClass = OverseasColorNumber.class; s.hasDeleted = true;
        s.searchCols = List.of("name", "color_name"); s.orderCols = List.of("id"); s.orderDesc = true;
        s.required = List.of("name");
        specs.put("color-numbers", s);
    }

    private Spec spec(String entity) {
        Spec s = specs.get(entity);
        if (s == null) {
            throw new OverseasException(404, "unknown formula entity: " + entity);
        }
        return s;
    }

    // ---- generic CRUD ----

    @Override
    public FormulaAdminPageResponse list(String entity, String keyword, Long brandId, Long modelId, int page, int pageSize) {
        Spec spec = spec(entity);
        int p = Math.max(1, page);
        int size = Math.min(200, Math.max(1, pageSize));
        QueryWrapper<Object> w = new QueryWrapper<>();
        if (spec.hasDeleted) {
            w.eq("is_deleted", "0");
        }
        if (keyword != null && !keyword.isBlank() && !spec.searchCols.isEmpty()) {
            String kw = keyword.trim();
            w.and(q -> {
                boolean first = true;
                for (String col : spec.searchCols) {
                    if (first) {
                        q.like(col, kw);
                        first = false;
                    } else {
                        q.or().like(col, kw);
                    }
                }
            });
        }
        Map<String, Long> filterValues = new HashMap<>();
        filterValues.put("brandId", brandId);
        filterValues.put("modelId", modelId);
        for (Map.Entry<String, String> e : spec.filterCols.entrySet()) {
            Long v = filterValues.get(e.getKey());
            if (v != null) {
                w.eq(e.getValue(), v);
            }
        }
        for (String col : spec.orderCols) {
            if (spec.orderDesc) {
                w.orderByDesc(col);
            } else {
                w.orderByAsc(col);
            }
        }
        IPage result = spec.dao.selectPage(new Page<>(p, size), w);
        FormulaAdminPageResponse resp = new FormulaAdminPageResponse();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object row : result.getRecords()) {
            rows.add(FormulaRowUtil.rowOut(row));
        }
        resp.setList(rows);
        resp.setTotal(result.getTotal());
        resp.setPage(p);
        resp.setPageSize(size);
        return resp;
    }

    @Override
    public Map<String, Object> create(String entity, Map<String, Object> body) {
        Spec spec = spec(entity);
        Object row = newEntity(spec);
        applyBody(row, body == null ? Map.of() : body);
        Date now = new Date();
        setIfPresent(row, "createTime", now);
        setIfPresent(row, "updateTime", now);
        if (fieldOf(row.getClass(), "enable") != null && getField(row, "enable") == null) {
            setField(row, "enable", "1");
        }
        validateRequired(entity, spec, row);
        validateUnique(entity, row, null);
        if (spec.inputId && getField(row, "id") == null) {
            setField(row, "id", nextId(spec));
        }
        spec.dao.insert(row);
        return FormulaRowUtil.rowOut(row);
    }

    @Override
    public Map<String, Object> update(String entity, Long id, Map<String, Object> body) {
        Spec spec = spec(entity);
        Object row = spec.dao.selectById(id);
        if (row == null) {
            throw new OverseasException(404, "record not found");
        }
        applyBody(row, body == null ? Map.of() : body);
        validateRequired(entity, spec, row);
        validateUnique(entity, row, id);
        setIfPresent(row, "updateTime", new Date());
        spec.dao.updateById(row);
        return FormulaRowUtil.rowOut(spec.dao.selectById(id));
    }

    @Override
    public void delete(String entity, Long id) {
        Spec spec = spec(entity);
        // Bug #12: a colour number that still has live formula lines
        // must not be deleted — its lines would be orphaned (the old UI
        // copy even warned they "won't be deleted automatically").
        if ("color-numbers".equals(entity)) {
            Long n = colorFormulaDao.selectCount(new LambdaQueryWrapper<OverseasColorFormula>()
                    .eq(OverseasColorFormula::getColornumId, id)
                    .eq(OverseasColorFormula::getIsDeleted, "0"));
            if (n != null && n > 0) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(),
                        "该色号下还有 " + n + " 条配方行，请先删除配方行后再删除色号");
            }
        }
        // Bug #13 (delete guard): a masterbatch still referenced by live
        // formula lines must not be deleted. Lines reference a masterbatch
        // two ways: rows created from the picker store its model, while the
        // legacy tinting data references it by NAME (the old model codes do
        // not exist in the masterbatch library at all). Checking the model
        // alone therefore never fired; check both.
        if ("masterbatches".equals(entity)) {
            Object mb = spec.dao.selectById(id);
            Object model = mb == null ? null : getField(mb, "model");
            Object name = mb == null ? null : getField(mb, "name");
            long used = 0;
            String usedBy = null;
            if (model != null && !String.valueOf(model).isBlank()) {
                Long n = colorFormulaDao.selectCount(new LambdaQueryWrapper<OverseasColorFormula>()
                        .eq(OverseasColorFormula::getModel, String.valueOf(model))
                        .eq(OverseasColorFormula::getIsDeleted, "0"));
                if (n != null && n > 0) {
                    used = n;
                    usedBy = "型号「" + model + "」";
                }
            }
            if (used == 0 && name != null && !String.valueOf(name).isBlank()) {
                Long n = colorFormulaDao.selectCount(new LambdaQueryWrapper<OverseasColorFormula>()
                        .eq(OverseasColorFormula::getName, String.valueOf(name))
                        .eq(OverseasColorFormula::getIsDeleted, "0"));
                if (n != null && n > 0) {
                    used = n;
                    usedBy = "名称「" + name + "」";
                }
            }
            if (used > 0) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(),
                        "色母" + usedBy + "已被 " + used + " 条配方行使用，请先清理配方行后再删除色母");
            }
        }
        spec.dao.deleteById(id);
    }

    // ---- required fields / uniqueness (batch-1 validation) ----

    private static final Map<String, String> FIELD_LABELS = Map.ofEntries(
            Map.entry("name", "名称"),
            Map.entry("code", "编码"),
            Map.entry("model", "型号"),
            Map.entry("engName", "英文名"),
            Map.entry("nameEn", "英文名"),
            Map.entry("language", "语言"),
            Map.entry("country", "国家"),
            Map.entry("brandId", "品牌"),
            Map.entry("modelId", "车型"),
            Map.entry("logo", "Logo"));

    private void validateRequired(String entity, Spec spec, Object row) {
        for (String field : spec.required) {
            Object v = getField(row, field);
            if (v == null || (v instanceof String && ((String) v).isBlank())) {
                String label = FIELD_LABELS.getOrDefault(field, field);
                if ("color-numbers".equals(entity) && "name".equals(field)) {
                    label = "色号";
                }
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), label + " 必填");
            }
        }
    }

    private void validateUnique(String entity, Object row, Long excludeId) {
        // Bug #11: (brandId, modelId, colorNum) unique among live
        // car-colors rows; a blank colorNum has nothing to dedupe on.
        if ("car-colors".equals(entity)) {
            Object brandId = getField(row, "brandId");
            Object modelId = getField(row, "modelId");
            Object colorNum = getField(row, "colorNum");
            if (brandId == null || modelId == null || colorNum == null || String.valueOf(colorNum).isBlank()) {
                return;
            }
            QueryWrapper<OverseasCarColor> w = new QueryWrapper<OverseasCarColor>()
                    .eq("brand_id", brandId)
                    .eq("model_id", modelId)
                    .eq("color_num", String.valueOf(colorNum))
                    .eq("is_deleted", "0");
            if (excludeId != null) {
                w.ne("id", excludeId);
            }
            Long n = carColorDao.selectCount(w);
            if (n != null && n > 0) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "该品牌+车型+色号已存在");
            }
        }
        // Bug #15: colour-card name unique among live rows.
        if ("color-cards".equals(entity)) {
            Object name = getField(row, "name");
            if (name == null || String.valueOf(name).isBlank()) {
                return;
            }
            QueryWrapper<OverseasColorCard> w = new QueryWrapper<OverseasColorCard>()
                    .eq("name", String.valueOf(name))
                    .eq("is_deleted", "0");
            if (excludeId != null) {
                w.ne("id", excludeId);
            }
            Long n = colorCardDao.selectCount(w);
            if (n != null && n > 0) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "色卡名称已存在");
            }
        }
    }

    // ---- colour numbers (same engine, fixed entity) ----

    @Override
    public FormulaAdminPageResponse listNumbers(String keyword, int page, int pageSize) {
        return list("color-numbers", keyword, null, null, page, pageSize);
    }

    @Override
    public Map<String, Object> createNumber(Map<String, Object> body) {
        return create("color-numbers", body);
    }

    @Override
    public Map<String, Object> updateNumber(Long id, Map<String, Object> body) {
        return update("color-numbers", id, body);
    }

    @Override
    public void deleteNumber(Long id) {
        delete("color-numbers", id);
    }

    // ---- formula lines of a colour number ----

    @Override
    public FormulaLinesResponse getLines(Long numberId) {
        requireNumber(numberId);
        return linesOf(numberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormulaLinesResponse replaceLines(Long numberId, FormulaLinesSaveRequest request) {
        requireNumber(numberId);
        if (request == null || request.getLines() == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "lines must be an array");
        }
        colorFormulaDao.delete(new LambdaQueryWrapper<OverseasColorFormula>()
                .eq(OverseasColorFormula::getColornumId, numberId));
        int i = 0;
        for (FormulaLineSaveRequest ln : request.getLines()) {
            OverseasColorFormula row = new OverseasColorFormula();
            row.setColornumId(numberId);
            row.setModel(ln.getModel() == null ? "" : ln.getModel());
            row.setName(ln.getName() == null ? "" : ln.getName());
            row.setDose(ln.getDose() == null ? 0.0 : ln.getDose());
            row.setColorRank(ln.getColorRank() == null ? i + 1 : ln.getColorRank());
            row.setEnable("1");
            row.setIsDeleted("0");
            row.setCreateTime(new Date());
            row.setUpdateTime(new Date());
            colorFormulaDao.insert(row);
            i++;
        }
        return linesOf(numberId);
    }

    private void requireNumber(Long numberId) {
        if (numberId == null || colorNumberDao.selectById(numberId) == null) {
            throw new OverseasException(404, "record not found");
        }
    }

    private FormulaLinesResponse linesOf(Long numberId) {
        List<OverseasColorFormula> lines = colorFormulaDao.selectList(
                new LambdaQueryWrapper<OverseasColorFormula>()
                        .eq(OverseasColorFormula::getColornumId, numberId)
                        .eq(OverseasColorFormula::getIsDeleted, "0")
                        .orderByAsc(OverseasColorFormula::getColorRank)
                        .orderByAsc(OverseasColorFormula::getId));
        FormulaLinesResponse resp = new FormulaLinesResponse();
        resp.setColorNumberId(String.valueOf(numberId));
        resp.setLines(lines.stream().map(l -> {
            FormulaLineResponse r = new FormulaLineResponse();
            r.setId(String.valueOf(l.getId()));
            r.setModel(l.getModel() == null ? "" : l.getModel());
            r.setName(l.getName() == null ? "" : l.getName());
            r.setDose(l.getDose());
            r.setColorRank(l.getColorRank());
            return r;
        }).collect(Collectors.toList()));
        return resp;
    }

    // ---- reflection helpers (request map -> entity, by field name) ----

    private Object newEntity(Spec spec) {
        try {
            return spec.entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new OverseasException("cannot instantiate " + spec.entityClass.getSimpleName());
        }
    }

    private Long nextId(Spec spec) {
        Object max = spec.dao.selectOne(new QueryWrapper<>().orderByDesc("id").last("LIMIT 1"));
        if (max == null) {
            return 1L;
        }
        Object id = getField(max, "id");
        return id == null ? 1L : ((Number) id).longValue() + 1;
    }

    private void applyBody(Object entity, Map<String, Object> body) {
        for (Field f : entity.getClass().getDeclaredFields()) {
            String name = f.getName();
            if (name.equals("id") || name.equals("serialVersionUID") || !body.containsKey(name)) {
                continue;
            }
            Object v = body.get(name);
            if (name.equals("enable")) {
                setField(entity, name, v == null ? null : FormulaRowUtil.flag(toBool(v)));
            } else {
                setField(entity, name, coerce(v, f.getType()));
            }
        }
    }

    private static Object coerce(Object v, Class<?> target) {
        if (v == null) {
            return null;
        }
        if (target == String.class) {
            return String.valueOf(v);
        }
        if (target == Long.class) {
            return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v).trim());
        }
        if (target == Integer.class) {
            return v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(String.valueOf(v).trim());
        }
        if (target == Double.class) {
            return v instanceof Number ? ((Number) v).doubleValue() : Double.parseDouble(String.valueOf(v).trim());
        }
        if (target == Boolean.class) {
            return toBool(v);
        }
        return v;
    }

    private static boolean toBool(Object v) {
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        }
        return "true".equalsIgnoreCase(String.valueOf(v)) || "1".equals(String.valueOf(v));
    }

    private static Field fieldOf(Class<?> cls, String name) {
        try {
            return cls.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static Object getField(Object target, String name) {
        Field f = fieldOf(target.getClass(), name);
        if (f == null) {
            return null;
        }
        try {
            f.setAccessible(true);
            return f.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void setField(Object target, String name, Object value) {
        Field f = fieldOf(target.getClass(), name);
        if (f == null) {
            return;
        }
        try {
            f.setAccessible(true);
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new OverseasException("cannot set field " + name);
        }
    }

    private static void setIfPresent(Object target, String name, Object value) {
        if (fieldOf(target.getClass(), name) != null) {
            setField(target, name, value);
        }
    }
}
