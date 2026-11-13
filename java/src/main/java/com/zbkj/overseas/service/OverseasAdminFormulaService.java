package com.zbkj.overseas.service;

import com.zbkj.overseas.request.FormulaLinesSaveRequest;
import com.zbkj.overseas.response.FormulaAdminPageResponse;
import com.zbkj.overseas.response.FormulaLinesResponse;

import java.util.Map;

/**
 * Admin paint-formula library (per-resource menu keys, see OverseasAdminServiceImpl.FORMULA_RESOURCE_PERM): config-driven CRUD over
 * the dictionary entities — car-brands, car-models, car-countries,
 * car-factories, color-cards, masterbatches, car-colors, color-brands —
 * plus colour numbers and their formula lines. Mirrors the Node
 * backend's FORMULA_CRUD table: request bodies are plain JSON maps,
 * responses are the same row maps (see FormulaRowUtil).
 */
public interface OverseasAdminFormulaService {

    FormulaAdminPageResponse list(String entity, String keyword, Long brandId, Long modelId, int page, int pageSize);

    Map<String, Object> create(String entity, Map<String, Object> body);

    Map<String, Object> update(String entity, Long id, Map<String, Object> body);

    void delete(String entity, Long id);

    FormulaAdminPageResponse listNumbers(String keyword, int page, int pageSize);

    Map<String, Object> createNumber(Map<String, Object> body);

    Map<String, Object> updateNumber(Long id, Map<String, Object> body);

    void deleteNumber(Long id);

    FormulaLinesResponse getLines(Long numberId);

    /** Replace the whole formula-line set of a colour number in one go. */
    FormulaLinesResponse replaceLines(Long numberId, FormulaLinesSaveRequest request);
}
