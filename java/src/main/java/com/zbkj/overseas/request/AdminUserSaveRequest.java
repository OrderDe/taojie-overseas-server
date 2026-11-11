package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Admin user create/update. username is only used on create; password is
 * required on create and means "reset password" when present on update.
 */
@Data
@Schema(name = "AdminUserSaveRequest", description = "后台用户保存（管理端）")
public class AdminUserSaveRequest {

    @Schema(description = "登录账号（仅新建时用）")
    private String username;

    @Schema(description = "密码（新建必填；编辑时填了才改）")
    private String password;

    @Schema(description = "显示名")
    private String displayName;

    @Schema(description = "角色 id")
    private Long roleId;

    @Schema(description = "是否启用")
    private Boolean active;
}
