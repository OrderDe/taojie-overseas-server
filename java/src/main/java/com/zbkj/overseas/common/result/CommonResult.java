package com.zbkj.overseas.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * Unified response wrapper — same shape as tjMall-common CommonResult:
 * { code, message, data }. The overseas PC frontend unwraps `data`,
 * exactly like the mini-program frontend does with tjmall responses.
 */
@Data
public class CommonResult<T> implements Serializable {

    private static final long serialVersionUID = -6630747483482976634L;

    private Integer code;
    private String message;
    private T data;

    private CommonResult() {
        this.code = 200;
    }

    private CommonResult(T data) {
        this.code = 200;
        this.data = data;
    }

    private CommonResult(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private CommonResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success() {
        return new CommonResult<>();
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(data);
    }

    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(CommonResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> CommonResult<T> failed() {
        return new CommonResult<>(CommonResultCode.FAILED.getCode(), CommonResultCode.FAILED.getMessage());
    }

    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(CommonResultCode.FAILED.getCode(), message);
    }

    public static <T> CommonResult<T> failed(IResultEnum resultEnum) {
        return new CommonResult<>(resultEnum.getCode(), resultEnum.getMessage());
    }

    public static <T> CommonResult<T> failed(Integer code, String message) {
        return new CommonResult<>(code, message);
    }
}
