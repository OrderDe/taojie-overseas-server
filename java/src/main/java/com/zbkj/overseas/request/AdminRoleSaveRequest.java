package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Admin role create/update — a role is a name plus its menu permissions.
 */
@Data
@Schema(name = "AdminRoleSaveRequest", description = "后台角色保存（管理端）")
public class AdminRoleSaveRequest {

    @Schema(description = "角色名")
    private String name;

    @Schema(description = "菜单权限 keys：products/categories/rfq/config/users，或 * 表示全部")
    private List<String> permissions;
}
