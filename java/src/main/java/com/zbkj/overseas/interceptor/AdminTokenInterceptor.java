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
 *      (menu permissions: products / categories / rfq / config / users).
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
        String need = adminService.requiredPerm(request.getRequestURI());
        // Product editors need the category list to fill the category dropdown.
        boolean categoryReadByProduct = "categories".equals(need)
                && "GET".equalsIgnoreCase(request.getMethod()) && principal.hasPerm("products");
        if (need != null && !categoryReadByProduct && !principal.hasPerm(need)) {
            throw new OverseasException(CommonResultCode.FORBIDDEN.getCode(), "no permission: " + need);
        }
        request.setAttribute(PRINCIPAL_ATTR, principal);
        return true;
    }
}
