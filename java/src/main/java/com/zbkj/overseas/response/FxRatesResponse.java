package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * FX rates for one base currency, as served to the admin price
 * calculator. {@code stale} means the upstream fetch failed and this is
 * the previous cache; {@code fromCache} means the row came from the
 * 12-hour cache without a new fetch.
 */
@Data
@Schema(name = "FxRatesResponse", description = "汇率表（后台试算工具）")
public class FxRatesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String base;

    private Map<String, Double> rates;

    private String sourceUpdatedAt;

    private String fetchedAt;

    private Boolean stale;

    private Boolean fromCache;
}
