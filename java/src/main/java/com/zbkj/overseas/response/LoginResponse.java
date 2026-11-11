package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Login result: the session token plus the account's menu permissions, so
 * the UI can render only the tabs the role allows.
 */
@Data
@Schema(name = "LoginResponse", description = "后台登录结果")
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话令牌（之后用 x-admin-token 头带上）")
    private String token;

    private AdminPrincipal user;
}
