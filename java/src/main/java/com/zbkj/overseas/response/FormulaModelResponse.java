package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Car model for the storefront formula pickers (linked to its brand).
 */
@Data
@Schema(name = "FormulaModelResponse", description = "配方库车型")
public class FormulaModelResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String brandId;

    private String model;
}
