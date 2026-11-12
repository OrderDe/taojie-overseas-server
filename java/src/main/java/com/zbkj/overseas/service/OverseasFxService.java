package com.zbkj.overseas.service;

import com.zbkj.overseas.response.FxRatesResponse;

/**
 * FX rates for the admin price calculator (open.er-api.com, 12h cache).
 * Same contract as the Node implementation's GET /api/admin/fx/rates.
 */
public interface OverseasFxService {

    FxRatesResponse rates(String base);
}
