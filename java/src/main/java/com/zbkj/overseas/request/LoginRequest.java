package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin login (username + password). Returns a session token that the UI
 * then sends as x-admin-token, exactly like the Node backend.
 */
@Data
@Schema(name = "LoginRequest", description = "后台登录")
public class LoginRequest {

    @NotBlank(message = "用户名必填")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "密码必填")
    @Schema(description = "密码")
    private String password;
}
