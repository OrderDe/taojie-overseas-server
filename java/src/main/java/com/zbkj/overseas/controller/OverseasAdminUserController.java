package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.interceptor.AdminTokenInterceptor;
import com.zbkj.overseas.request.AdminRoleSaveRequest;
import com.zbkj.overseas.request.AdminUserSaveRequest;
import com.zbkj.overseas.request.LoginRequest;
import com.zbkj.overseas.response.AdminPrincipal;
import com.zbkj.overseas.response.AdminRoleResponse;
import com.zbkj.overseas.response.AdminUserResponse;
import com.zbkj.overseas.response.LoginResponse;
import com.zbkj.overseas.response.PermissionResponse;
import com.zbkj.overseas.service.OverseasAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin accounts controller: login / logout / me plus user and role
 * management. Everything except /login sits behind AdminTokenInterceptor,
 * and the users/roles endpoints additionally need the "users" permission.
 * Same paths and shapes as the Node backend.
 */
@Slf4j
@RestController
@RequestMapping("api/admin")
@Tag(name = "海外后台用户与角色控制器")
public class OverseasAdminUserController {

    @Autowired
    private OverseasAdminService adminService;

    @Operation(summary = "后台登录（账号+密码 → 会话令牌与菜单权限）")
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return CommonResult.success(adminService.login(request));
    }

    @Operation(summary = "退出登录（作废当前会话）")
    @PostMapping("/logout")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        adminService.logout(request.getHeader("x-admin-token"));
        return CommonResult.success(true);
    }

    @Operation(summary = "当前登录管理员（含菜单权限）")
    @GetMapping("/me")
    public CommonResult<AdminPrincipal> me(HttpServletRequest request) {
        return CommonResult.success(
                (AdminPrincipal) request.getAttribute(AdminTokenInterceptor.PRINCIPAL_ATTR));
    }

    @Operation(summary = "菜单权限定义（角色编辑用）")
    @GetMapping("/permissions")
    public CommonResult<List<PermissionResponse>> permissions() {
        return CommonResult.success(adminService.permissionDefs());
    }

    // ---- users ----

    @Operation(summary = "后台用户列表")
    @GetMapping("/users")
    public CommonResult<List<AdminUserResponse>> listUsers() {
        return CommonResult.success(adminService.listUsers());
    }

    @Operation(summary = "新建后台用户")
    @PostMapping("/users")
    public CommonResult<AdminUserResponse> createUser(@RequestBody AdminUserSaveRequest request) {
        return CommonResult.success(adminService.saveUser(request, null));
    }

    @Operation(summary = "编辑后台用户（改显示名/角色/启用状态，填密码则重置密码）")
    @PutMapping("/users/{id}")
    public CommonResult<AdminUserResponse> updateUser(@RequestBody AdminUserSaveRequest request,
                                                      @PathVariable Long id) {
        return CommonResult.success(adminService.saveUser(request, id));
    }

    @Operation(summary = "删除后台用户")
    @DeleteMapping("/users/{id}")
    public CommonResult<Boolean> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        AdminPrincipal me = (AdminPrincipal) request.getAttribute(AdminTokenInterceptor.PRINCIPAL_ATTR);
        adminService.deleteUser(id, me == null ? null : me.getUserId());
        return CommonResult.success(true);
    }

    // ---- roles ----

    @Operation(summary = "角色列表（含每个角色的用户数）")
    @GetMapping("/roles")
    public CommonResult<List<AdminRoleResponse>> listRoles() {
        return CommonResult.success(adminService.listRoles());
    }

    @Operation(summary = "新建角色")
    @PostMapping("/roles")
    public CommonResult<AdminRoleResponse> createRole(@RequestBody AdminRoleSaveRequest request) {
        return CommonResult.success(adminService.saveRole(request, null));
    }

    @Operation(summary = "编辑角色（内置超级管理员的权限固定为全部）")
    @PutMapping("/roles/{id}")
    public CommonResult<AdminRoleResponse> updateRole(@RequestBody AdminRoleSaveRequest request,
                                                      @PathVariable Long id) {
        return CommonResult.success(adminService.saveRole(request, id));
    }

    @Operation(summary = "删除角色（内置角色、仍有用户的角色不可删）")
    @DeleteMapping("/roles/{id}")
    public CommonResult<Boolean> deleteRole(@PathVariable Long id) {
        adminService.deleteRole(id);
        return CommonResult.success(true);
    }
}
