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
    RATE_LIMITED(6004, "提交过于频繁，请稍后再试"),
    CHAT_SESSION_INVALID(6005, "聊天会话不存在或已失效"),
    CHAT_MESSAGE_EMPTY(6006, "消息内容不能为空"),
    VISITOR_NOT_FOUND(6007, "访客不存在"),
    FORMULA_NOT_FOUND(6008, "颜色不存在"),
    PAGE_NOT_FOUND(6009, "页面不存在或已隐藏");

    private final Integer code;
    private final String message;

    OverseasResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
