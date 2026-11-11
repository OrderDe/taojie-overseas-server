package com.zbkj.overseas.common.result;

import lombok.Getter;

/**
 * Shared result codes (subset) — mirrors tjMall-common CommonResultCode.
 */
@Getter
public enum CommonResultCode implements IResultEnum {
    SUCCESS(200, "成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "暂未登录或 token 已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在");

    private final Integer code;
    private final String message;

    CommonResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
