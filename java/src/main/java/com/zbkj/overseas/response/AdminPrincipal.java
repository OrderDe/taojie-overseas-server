package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * The logged-in admin as the UI sees it (also the /me response and the
 * `user` half of the login response). The interceptor stores the resolved
 * principal on the request under the attribute name "adminPrincipal".
 */
@Data
@Schema(name = "AdminPrincipal", description = "当前登录管理员")
public class AdminPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0 for the break-glass root token (not a database account). */
    private Long userId;

    private String username;

    private String displayName;

    private String roleName;

    @Schema(description = "菜单权限 keys，含 * 表示全部")
    private List<String> permissions;

    public boolean hasPerm(String key) {
        return permissions != null && (permissions.contains("*") || permissions.contains(key));
    }
}
