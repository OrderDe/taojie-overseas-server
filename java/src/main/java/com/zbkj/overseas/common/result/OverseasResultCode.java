package com.zbkj.overseas.common.result;

import lombok.Getter;

/**
 * Overseas-mall specific result codes, following tjmall's per-domain
 * result-code pattern (ProductResultCode, OrderResultCode, ...).
 */
@Getter
public enum OverseasResultCode implements IResultEnum {
    PRODUCT_NOT_FOUND(6001, "商品不存在或已下架"),
    RFQ_CONTACT_REQUIRED(6002, "询价必须填写联系方式"),
    ADMIN_TOKEN_INVALID(6003, "管理口令无效"),
    RATE_LIMITED(6004, "提交过于频繁，请稍后再试");

    private final Integer code;
    private final String message;

    OverseasResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
