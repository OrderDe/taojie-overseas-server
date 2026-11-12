package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Name auto-translate for the admin forms: the operator types only the
 * Chinese name; the server returns en/es/fr/pt to store in the names JSON.
 * Source language is fixed to zh.
 */
@Data
@Schema(name = "TranslateRequest", description = "名称自动翻译请求")
public class TranslateRequest {

    @NotBlank(message = "待翻译文本不能为空")
    @Schema(description = "中文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String text;

    @Schema(description = "目标语种（en/es/fr/pt），为空默认四种全译")
    private List<String> targets;
}
