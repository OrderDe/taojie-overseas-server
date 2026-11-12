package com.zbkj.overseas.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * FX rate cache for the admin price calculator — one row per base
 * currency, refreshed from open.er-api.com at most every 12 hours.
 * Mirrors the Node implementation's fx_rates table.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_fx_rate")
public class OverseasFxRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "base", type = IdType.INPUT)
    private String base;

    /** Rates JSON: {currency code: units per 1 base}. */
    private String rates;

    /** Source-side update time, ISO-8601. */
    private String sourceUpdatedAt;

    /** When this row was fetched, ISO-8601. */
    private String fetchedAt;
}
