package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Image upload (admin). Base64-over-JSON, same contract as the Node backend,
 * so the shared admin UI works against either implementation.
 */
@Data
@Schema(name = "UploadRequest", description = "图片上传（管理端，base64）")
public class UploadRequest {

    @Schema(description = "原始文件名（取扩展名与安全化后的文件名）")
    private String filename;

    @Schema(description = "图片内容的 base64（不带 data: 前缀），上限 6 MB")
    private String dataBase64;
}
