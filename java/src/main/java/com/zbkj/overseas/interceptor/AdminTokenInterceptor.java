package com.zbkj.overseas.interceptor;

import cn.hutool.core.util.StrUtil;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.response.AdminPrincipal;
import com.zbkj.overseas.service.OverseasAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Admin guard for /api/admin/** (the login endpoint is excluded in
 * WebMvcConfig). Two credentials, checked in order, both arriving in the
 * `x-admin-token` header:
 *   1. the shared ADMIN_TOKEN — break-glass root, full permissions;
 *   2. a login session token — permissions come from the account's role
 *      (one menu-permission key per sidebar menu).
 * The resolved principal is stored as request attribute "adminPrincipal".
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_ATTR = "adminPrincipal";

    @Value("${overseas.admin-token:}")
    private String adminToken;

    @Autowired
    private OverseasAdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("x-admin-token");
        AdminPrincipal principal;
        if (StrUtil.isNotBlank(adminToken) && adminToken.equals(token)) {
            principal = adminService.rootPrincipal();
        } else {
            principal = adminService.resolve(token);
        }
        if (principal == null) {
            throw new OverseasException(OverseasResultCode.ADMIN_TOKEN_INVALID);
        }
        String path = request.getRequestURI();
        // Any-of paths (upload): holding any one of the keys is enough.
        java.util.List<String> anyOf = adminService.requiredPermAnyOf(path);
        if (anyOf != null) {
            if (anyOf.stream().noneMatch(principal::hasPerm)) {
                throw new OverseasException(CommonResultCode.FORBIDDEN.getCode(),
                        "no permission: " + String.join("/", anyOf));
            }
            request.setAttribute(PRINCIPAL_ATTR, principal);
            return true;
        }
        String need = adminService.requiredPerm(path);
        // Read-only cross-menu exceptions (dropdown / lookup data other
        // editors legitimately read; writes always need the owning key):
        //  - the category list feeds the product editor's dropdown and
        //    the navigation / footer link pickers;
        //  - the custom-page list feeds the navigation / footer pickers;
        //  - formula pages read each other's tables for selects, so any
        //    formula key may read any formula resource.
        boolean get = "GET".equalsIgnoreCase(request.getMethod());
        boolean crossRead = get && (
                ("categories".equals(need) && (principal.hasPerm("goods")
                        || principal.hasPerm("navigation") || principal.hasPerm("footer")))
                || ("pages".equals(need) && (principal.hasPerm("navigation") || principal.hasPerm("footer")))
                || (adminService.isFormulaGroupKey(need) && hasAnyFormulaKey(principal)));
        if (need != null && !crossRead && !principal.hasPerm(need)) {
            throw new OverseasException(CommonResultCode.FORBIDDEN.getCode(), "no permission: " + need);
        }
        request.setAttribute(PRINCIPAL_ATTR, principal);
        return true;
    }

    private boolean hasAnyFormulaKey(AdminPrincipal principal) {
        return principal.getPermissions() != null && principal.getPermissions().stream()
                .anyMatch(adminService::isFormulaGroupKey);
    }
}
