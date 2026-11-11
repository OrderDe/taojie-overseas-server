package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Brand row returned to the storefront (only switched-on brands).
 */
@Data
@Schema(name = "BrandResponse", description = "海外品牌")
public class BrandResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    @Schema(description = "按请求语言解析后的品牌名")
    private String name;

    private String logo;
}
