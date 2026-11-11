package com.zbkj.overseas.common.exception;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.common.result.CommonResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler — mirrors tjMall-common com.zbkj.common.exception.
 * ExceptionHandler: every failure leaves as a CommonResult, never a stack page.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OverseasException.class)
    public CommonResult<String> handle(OverseasException e) {
        log.warn("business exception code={} msg={}", e.getCode(), e.getMessage());
        return CommonResult.failed(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public CommonResult<String> handleValidation(Exception e) {
        FieldError fieldError = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldError()
                : ((BindException) e).getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage()
                : CommonResultCode.VALIDATE_FAILED.getMessage();
        return CommonResult.failed(CommonResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<String> handleAll(Exception e) {
        log.error("unhandled exception", e);
        return CommonResult.failed();
    }
}
