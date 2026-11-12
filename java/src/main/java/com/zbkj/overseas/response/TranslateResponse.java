package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Per-language results: a language that failed is listed in {@code errors}
 * and omitted from {@code translations} — one bad language never fails
 * the whole request.
 */
@Data
@Schema(name = "TranslateResponse", description = "名称自动翻译结果")
public class TranslateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> translations;

    private Map<String, String> errors;
}
