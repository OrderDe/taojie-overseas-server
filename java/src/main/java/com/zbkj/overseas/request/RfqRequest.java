package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RFQ submit request. Validation messages go through the global handler,
 * same as tjmall's validated front requests.
 */
@Data
@Schema(name = "RfqRequest", description = "海外询价提交")
public class RfqRequest {

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "公司名")
    private String company;

    @Schema(description = "国家/地区")
    private String country;

    @NotBlank(message = "联系方式不能为空")
    @Schema(description = "邮箱 / WhatsApp / 电话", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contact;

    @Schema(description = "意向商品名")
    private String productName;

    @Schema(description = "数量")
    private String quantity;

    @Schema(description = "留言")
    private String message;
}
