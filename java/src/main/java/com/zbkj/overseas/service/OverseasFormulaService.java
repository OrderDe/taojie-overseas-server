package com.zbkj.overseas.service;

import com.zbkj.overseas.response.FormulaBrandResponse;
import com.zbkj.overseas.response.FormulaColorDetailResponse;
import com.zbkj.overseas.response.FormulaColorPageResponse;
import com.zbkj.overseas.response.FormulaModelResponse;

import java.util.List;

/**
 * Storefront paint-formula library (public): brand/model pickers,
 * car-colour search and the colour detail with LAB + formula lines.
 * Same routes and JSON shapes as the Node backend.
 */
public interface OverseasFormulaService {

    /** Enabled car brands, ordered by letter_rank. */
    List<FormulaBrandResponse> brands();

    /** Enabled car models; all of them when brandId is null. */
    List<FormulaModelResponse> models(Long brandId);

    /** Car-colour search over enabled, non-deleted rows. */
    FormulaColorPageResponse search(Long brandId, Long modelId, String keyword, int page, int pageSize);

    /**
     * Colour detail: the car-colour row plus the colour numbers linked
     * through the relation table (relation.carColor = car_color.id,
     * relation.colorNumber = color_number.id — verified against the
     * imported data), each with LAB values and its formula lines.
     */
    FormulaColorDetailResponse detail(Long id);
}
