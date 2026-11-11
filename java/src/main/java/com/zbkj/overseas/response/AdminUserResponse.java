package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Admin account as listed in the user-management tab (never the hash).
 */
@Data
@Schema(name = "AdminUserResponse", description = "后台用户（管理端）")
public class AdminUserResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String displayName;

    private Long roleId;

    private String roleName;

    private Boolean active;

    private Date createdAt;
}
