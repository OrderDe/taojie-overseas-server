package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Image / video upload (admin). Base64-over-JSON, same contract as the Node
 * backend, so the shared admin UI works against either implementation.
 * A 50 MB video arrives as ~68 MB of base64 JSON, so this endpoint's body is
 * not capped by the multipart settings (those only gate file-form uploads);
 * the controller enforces the per-type byte limits after decoding.
 */
@Data
@Schema(name = "UploadRequest", description = "图片/视频上传（管理端，base64）")
public class UploadRequest {

    @Schema(description = "原始文件名（取扩展名与安全化后的文件名）")
    private String filename;

    @Schema(description = "文件内容的 base64（不带 data: 前缀）；图片上限 6 MB，视频（mp4/webm/mov）上限 50 MB")
    private String dataBase64;
}
