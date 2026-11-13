package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Enabled car brand for the storefront formula pickers.
 * id is a string: the source data carries snowflake ids beyond the
 * exact range of a JSON number in the browser.
 */
@Data
@Schema(name = "FormulaBrandResponse", description = "配方库汽车品牌")
public class FormulaBrandResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;
}
