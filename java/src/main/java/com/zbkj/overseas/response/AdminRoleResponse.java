package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Admin role as listed in the user-management tab, with its live user count
 * (the delete guard uses the same count).
 */
@Data
@Schema(name = "AdminRoleResponse", description = "后台角色（管理端）")
public class AdminRoleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    @Schema(description = "菜单权限 keys")
    private List<String> permissions;

    private Boolean builtin;

    private Integer userCount;
}
