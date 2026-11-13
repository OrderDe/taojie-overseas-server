package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasAdminRoleDao;
import com.zbkj.overseas.dao.OverseasAdminSessionDao;
import com.zbkj.overseas.dao.OverseasAdminUserDao;
import com.zbkj.overseas.model.OverseasAdminRole;
import com.zbkj.overseas.model.OverseasAdminSession;
import com.zbkj.overseas.model.OverseasAdminUser;
import com.zbkj.overseas.request.AdminRoleSaveRequest;
import com.zbkj.overseas.request.AdminUserSaveRequest;
import com.zbkj.overseas.request.LoginRequest;
import com.zbkj.overseas.response.AdminPrincipal;
import com.zbkj.overseas.response.AdminRoleResponse;
import com.zbkj.overseas.response.AdminUserResponse;
import com.zbkj.overseas.response.LoginResponse;
import com.zbkj.overseas.response.PermissionResponse;
import com.zbkj.overseas.service.OverseasAdminService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin accounts implementation.
 * Passwords use PBKDF2WithHmacSHA256 from the JDK (no extra dependency);
 * sessions are random tokens in a table, mirroring the Node backend —
 * including the bootstrap rule: with an empty user table, the first
 * account is admin / ADMIN_TOKEN with the super-admin role.
 */
@Slf4j
@Service
public class OverseasAdminServiceImpl extends ServiceImpl<OverseasAdminUserDao, OverseasAdminUser>
        implements OverseasAdminService {

    private static final List<PermissionResponse> PERMISSION_DEFS = List.of(
            new PermissionResponse("products", "商品管理"),
            new PermissionResponse("categories", "分类管理"),
            new PermissionResponse("rfq", "询价列表"),
            new PermissionResponse("chat", "在线聊天"),
            new PermissionResponse("visitors", "访客统计"),
            new PermissionResponse("formula", "配方管理"),
            new PermissionResponse("config", "站点配置"),
            new PermissionResponse("users", "用户与角色"));
    private static final List<String> ALL_PERMS =
            PERMISSION_DEFS.stream().map(PermissionResponse::getKey).collect(Collectors.toList());
    private static final long SESSION_TTL_MS = 7L * 24 * 3600 * 1000;
    private static final int PBKDF2_ITERATIONS = 120_000;

    @Autowired
    private OverseasAdminRoleDao roleDao;

    @Autowired
    private OverseasAdminSessionDao sessionDao;

    @Value("${overseas.admin-token:}")
    private String adminToken;

    // ---- bootstrap ----

    @PostConstruct
    public void bootstrap() {
        if (roleDao.selectCount(new LambdaQueryWrapper<OverseasAdminRole>()) == 0) {
            insertRole("超级管理员", List.of("*"), true);
            insertRole("运营", List.of("products", "categories", "rfq", "chat", "config", "visitors", "formula"), false);
            insertRole("外贸员", List.of("rfq", "chat", "visitors"), false);
        }
        if (count() == 0 && StrUtil.isNotBlank(adminToken)) {
            OverseasAdminRole superRole = roleDao.selectOne(new LambdaQueryWrapper<OverseasAdminRole>()
                    .eq(OverseasAdminRole::getBuiltin, true).last("LIMIT 1"));
            if (superRole != null) {
                OverseasAdminUser user = new OverseasAdminUser();
                user.setUsername("admin");
                user.setPasswordHash(hashPassword(adminToken));
                user.setDisplayName("管理员");
                user.setRoleId(superRole.getId());
                user.setActive(true);
                user.setCreateTime(new Date());
                user.setUpdateTime(new Date());
                save(user);
                log.info("created initial admin account: admin (password = ADMIN_TOKEN)");
            }
        }
    }

    private void insertRole(String name, List<String> permissions, boolean builtin) {
        OverseasAdminRole role = new OverseasAdminRole();
        role.setName(name);
        role.setPermissions(JSONUtil.toJsonStr(permissions));
        role.setBuiltin(builtin);
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        roleDao.insert(role);
    }

    // ---- login / session ----

    @Override
    public List<PermissionResponse> permissionDefs() {
        return PERMISSION_DEFS;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        OverseasAdminUser user = getOne(new LambdaQueryWrapper<OverseasAdminUser>()
                .eq(OverseasAdminUser::getUsername, StrUtil.trim(request.getUsername())), false);
        if (user == null || !Boolean.TRUE.equals(user.getActive())
                || !verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new OverseasException(OverseasResultCode.ADMIN_TOKEN_INVALID.getCode(), "账号或密码不正确");
        }
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String token = HexFormat.of().formatHex(raw);
        OverseasAdminSession session = new OverseasAdminSession();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setExpiresAt(System.currentTimeMillis() + SESSION_TTL_MS);
        sessionDao.insert(session);
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(toPrincipal(user));
        return response;
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isNotBlank(token)) {
            sessionDao.deleteById(token);
        }
    }

    @Override
    public AdminPrincipal resolve(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        OverseasAdminSession session = sessionDao.selectById(token);
        if (session == null || session.getExpiresAt() == null
                || session.getExpiresAt() < System.currentTimeMillis()) {
            return null;
        }
        OverseasAdminUser user = getById(session.getUserId());
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            return null;
        }
        return toPrincipal(user);
    }

    @Override
    public AdminPrincipal rootPrincipal() {
        AdminPrincipal p = new AdminPrincipal();
        p.setUserId(0L);
        p.setUsername("root");
        p.setDisplayName("超级管理员（令牌）");
        p.setRoleName("超级管理员");
        p.setPermissions(List.of("*"));
        return p;
    }

    @Override
    public String requiredPerm(String path) {
        if (path.equals("/api/admin/me") || path.equals("/api/admin/logout")
                || path.equals("/api/admin/fx/rates")) {
            return null;
        }
        if (path.startsWith("/api/admin/users") || path.startsWith("/api/admin/roles")
                || path.equals("/api/admin/permissions")) {
            return "users";
        }
        if (path.startsWith("/api/admin/products") || path.startsWith("/api/admin/brands")
                || path.equals("/api/admin/upload")) {
            return "products";
        }
        if (path.startsWith("/api/admin/categories")) {
            return "categories";
        }
        if (path.equals("/api/admin/rfq")) {
            return "rfq";
        }
        if (path.startsWith("/api/admin/chat")) {
            return "chat";
        }
        if (path.startsWith("/api/admin/visitors")) {
            return "visitors";
        }
        if (path.startsWith("/api/admin/formula")) {
            return "formula";
        }
        if (path.equals("/api/admin/site-config")) {
            return "config";
        }
        return null;
    }

    private AdminPrincipal toPrincipal(OverseasAdminUser user) {
        OverseasAdminRole role = roleDao.selectById(user.getRoleId());
        AdminPrincipal p = new AdminPrincipal();
        p.setUserId(user.getId());
        p.setUsername(user.getUsername());
        p.setDisplayName(user.getDisplayName());
        p.setRoleName(role == null ? "" : role.getName());
        p.setPermissions(role == null ? List.of() : parsePerms(role.getPermissions()));
        return p;
    }

    // ---- users ----

    @Override
    public List<AdminUserResponse> listUsers() {
        Map<Long, String> roleNames = roleDao.selectList(new LambdaQueryWrapper<OverseasAdminRole>()).stream()
                .collect(Collectors.toMap(OverseasAdminRole::getId, OverseasAdminRole::getName));
        return list(new LambdaQueryWrapper<OverseasAdminUser>().orderByAsc(OverseasAdminUser::getId))
                .stream().map(u -> toUserResponse(u, roleNames)).collect(Collectors.toList());
    }

    @Override
    public AdminUserResponse saveUser(AdminUserSaveRequest request, Long id) {
        OverseasAdminUser user;
        if (id == null) {
            String username = StrUtil.trim(request.getUsername());
            if (username == null || !username.matches("[a-zA-Z0-9_.-]{2,32}")) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "账号需为 2-32 位字母、数字或 _ . -");
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "密码至少 6 位");
            }
            if (getOne(new LambdaQueryWrapper<OverseasAdminUser>()
                    .eq(OverseasAdminUser::getUsername, username), false) != null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "账号已存在");
            }
            user = new OverseasAdminUser();
            user.setUsername(username);
            user.setPasswordHash(hashPassword(request.getPassword()));
            user.setCreateTime(new Date());
        } else {
            user = getById(id);
            if (user == null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "用户不存在");
            }
            if (StrUtil.isNotBlank(request.getPassword())) {
                if (request.getPassword().length() < 6) {
                    throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "密码至少 6 位");
                }
                user.setPasswordHash(hashPassword(request.getPassword()));
            }
        }
        Long roleId = request.getRoleId() == null ? user.getRoleId() : request.getRoleId();
        if (roleId == null || roleDao.selectById(roleId) == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "角色不存在");
        }
        boolean active = request.getActive() == null
                ? !Boolean.FALSE.equals(user.getActive()) : request.getActive();
        if (id != null && Boolean.TRUE.equals(user.getActive()) && isSuperRole(user.getRoleId())
                && (!active || !isSuperRole(roleId)) && !otherSuperExists(id)) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "至少保留一个启用的超级管理员");
        }
        user.setDisplayName(request.getDisplayName() == null ? user.getDisplayName() : request.getDisplayName());
        user.setRoleId(roleId);
        user.setActive(active);
        user.setUpdateTime(new Date());
        saveOrUpdate(user);
        if (!active) {
            sessionDao.delete(new LambdaQueryWrapper<OverseasAdminSession>()
                    .eq(OverseasAdminSession::getUserId, user.getId()));
        }
        return toUserResponse(user, null);
    }

    @Override
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "不能删除自己的账号");
        }
        OverseasAdminUser user = getById(id);
        if (user == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "用户不存在");
        }
        if (Boolean.TRUE.equals(user.getActive()) && isSuperRole(user.getRoleId()) && !otherSuperExists(id)) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "至少保留一个启用的超级管理员");
        }
        sessionDao.delete(new LambdaQueryWrapper<OverseasAdminSession>()
                .eq(OverseasAdminSession::getUserId, id));
        removeById(id);
    }

    private AdminUserResponse toUserResponse(OverseasAdminUser user, Map<Long, String> roleNames) {
        AdminUserResponse r = new AdminUserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setDisplayName(user.getDisplayName());
        r.setRoleId(user.getRoleId());
        if (roleNames != null) {
            r.setRoleName(roleNames.getOrDefault(user.getRoleId(), ""));
        } else {
            OverseasAdminRole role = roleDao.selectById(user.getRoleId());
            r.setRoleName(role == null ? "" : role.getName());
        }
        r.setActive(user.getActive());
        r.setCreatedAt(user.getCreateTime());
        return r;
    }

    private boolean isSuperRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        OverseasAdminRole role = roleDao.selectById(roleId);
        return role != null && parsePerms(role.getPermissions()).contains("*");
    }

    private boolean otherSuperExists(Long excludeUserId) {
        return list(new LambdaQueryWrapper<OverseasAdminUser>()
                .eq(OverseasAdminUser::getActive, true)
                .ne(OverseasAdminUser::getId, excludeUserId))
                .stream().anyMatch(u -> isSuperRole(u.getRoleId()));
    }

    // ---- roles ----

    @Override
    public List<AdminRoleResponse> listRoles() {
        return roleDao.selectList(new LambdaQueryWrapper<OverseasAdminRole>()
                        .orderByAsc(OverseasAdminRole::getId))
                .stream().map(this::toRoleResponse).collect(Collectors.toList());
    }

    @Override
    public AdminRoleResponse saveRole(AdminRoleSaveRequest request, Long id) {
        String name = StrUtil.trim(request.getName());
        if (StrUtil.isBlank(name)) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "角色名必填");
        }
        OverseasAdminRole role;
        if (id == null) {
            if (roleDao.selectOne(new LambdaQueryWrapper<OverseasAdminRole>()
                    .eq(OverseasAdminRole::getName, name)) != null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "角色名已存在");
            }
            role = new OverseasAdminRole();
            role.setBuiltin(false);
            role.setCreateTime(new Date());
        } else {
            role = roleDao.selectById(id);
            if (role == null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "角色不存在");
            }
            OverseasAdminRole dup = roleDao.selectOne(new LambdaQueryWrapper<OverseasAdminRole>()
                    .eq(OverseasAdminRole::getName, name).ne(OverseasAdminRole::getId, id));
            if (dup != null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "角色名已存在");
            }
        }
        role.setName(name);
        // The built-in super-admin role always keeps full permissions.
        List<String> perms = Boolean.TRUE.equals(role.getBuiltin())
                ? List.of("*")
                : filterPerms(request.getPermissions(), role.getPermissions());
        role.setPermissions(JSONUtil.toJsonStr(perms));
        role.setUpdateTime(new Date());
        if (role.getId() == null) {
            roleDao.insert(role);
        } else {
            roleDao.updateById(role);
        }
        return toRoleResponse(role);
    }

    @Override
    public void deleteRole(Long id) {
        OverseasAdminRole role = roleDao.selectById(id);
        if (role == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "角色不存在");
        }
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "内置角色不能删除");
        }
        Long n = count(new LambdaQueryWrapper<OverseasAdminUser>().eq(OverseasAdminUser::getRoleId, id));
        if (n != null && n > 0) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(),
                    "该角色下还有 " + n + " 个用户，请先给他们换角色");
        }
        roleDao.deleteById(id);
    }

    private AdminRoleResponse toRoleResponse(OverseasAdminRole role) {
        AdminRoleResponse r = new AdminRoleResponse();
        r.setId(role.getId());
        r.setName(role.getName());
        r.setPermissions(parsePerms(role.getPermissions()));
        r.setBuiltin(role.getBuiltin());
        Long n = count(new LambdaQueryWrapper<OverseasAdminUser>().eq(OverseasAdminUser::getRoleId, role.getId()));
        r.setUserCount(n == null ? 0 : n.intValue());
        return r;
    }

    private List<String> filterPerms(List<String> requested, String currentJson) {
        if (requested == null) {
            return parsePerms(currentJson);
        }
        return requested.stream().filter(p -> ALL_PERMS.contains(p) || "*".equals(p)).collect(Collectors.toList());
    }

    // ---- password hashing (JDK PBKDF2, no extra dependency) ----

    static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS);
            Base64.Encoder enc = Base64.getEncoder();
            return "pbkdf2:" + PBKDF2_ITERATIONS + ":" + enc.encodeToString(salt) + ":" + enc.encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("password hashing failed", e);
        }
    }

    static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = (stored == null ? "" : stored).split(":");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password == null ? "" : password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    static List<String> parsePerms(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
