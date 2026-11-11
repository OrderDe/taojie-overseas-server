package com.zbkj.overseas.common.exception;

import com.zbkj.overseas.common.result.IResultEnum;
import lombok.Getter;

/**
 * Business exception — mirrors tjmall's TjmallException usage:
 * services throw it with a result-code enum, the global handler renders it.
 */
@Getter
public class OverseasException extends RuntimeException {

    private final Integer code;

    public OverseasException(IResultEnum resultEnum) {
        super(resultEnum.getMessage());
        this.code = resultEnum.getCode();
    }

    public OverseasException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public OverseasException(String message) {
        super(message);
        this.code = 500;
    }
}
