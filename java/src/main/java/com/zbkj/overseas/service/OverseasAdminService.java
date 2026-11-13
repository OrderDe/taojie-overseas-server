package com.zbkj.overseas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.overseas.model.OverseasAdminUser;
import com.zbkj.overseas.request.AdminRoleSaveRequest;
import com.zbkj.overseas.request.AdminUserSaveRequest;
import com.zbkj.overseas.request.LoginRequest;
import com.zbkj.overseas.request.SiteConfigRequest;
import com.zbkj.overseas.response.AdminPrincipal;
import com.zbkj.overseas.response.AdminRoleResponse;
import com.zbkj.overseas.response.AdminUserResponse;
import com.zbkj.overseas.response.LoginResponse;
import com.zbkj.overseas.response.PermissionResponse;

import java.util.List;

/**
 * Admin accounts service — user login, sessions, and role (menu
 * permission) management for the overseas management UI. Same contract
 * as the Node backend: the shared ADMIN_TOKEN remains a break-glass root
 * credential, accounts log in for a session token, roles carry one
 * permission key per sidebar menu.
 */
public interface OverseasAdminService extends IService<OverseasAdminUser> {

    /** The menu permissions (one per sidebar menu), in UI order. */
    List<PermissionResponse> permissionDefs();

    /** Username + password login; throws OverseasException on bad credentials. */
    LoginResponse login(LoginRequest request);

    /** Invalidate one session token (no-op for the root token). */
    void logout(String token);

    /** Resolve a session token to its principal; null when invalid/expired/disabled. */
    AdminPrincipal resolve(String token);

    /** The principal for the break-glass root ADMIN_TOKEN. */
    AdminPrincipal rootPrincipal();

    /** Permission key a /api/admin path needs, or null for "any account". */
    String requiredPerm(String path);

    /** For paths any one of several keys opens (the upload endpoint):
     *  the acceptable keys, or null when requiredPerm governs the path. */
    List<String> requiredPermAnyOf(String path);

    /** Whether a permission key belongs to the formula menu group. */
    boolean isFormulaGroupKey(String key);

    /**
     * PUT /api/admin/site-config is shared by five settings pages, each
     * owning a slice of the config. Returns the menu key the principal
     * still needs for the slices this request touches, or null when the
     * save is allowed.
     */
    String siteConfigSaveDenial(SiteConfigRequest request, AdminPrincipal principal);

    List<AdminUserResponse> listUsers();

    /** Create (id == null) or update an admin account. */
    AdminUserResponse saveUser(AdminUserSaveRequest request, Long id);

    /** Delete an account; refuses self-deletion and removing the last super admin. */
    void deleteUser(Long id, Long currentUserId);

    List<AdminRoleResponse> listRoles();

    /** Create (id == null) or update a role. The built-in super role keeps ["*"]. */
    AdminRoleResponse saveRole(AdminRoleSaveRequest request, Long id);

    /** Delete a role; refuses built-in roles and roles still assigned to users. */
    void deleteRole(Long id);
}
