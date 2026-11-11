package com.zbkj.overseas.interceptor;

import cn.hutool.core.util.StrUtil;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.OverseasResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Admin guard for /api/admin/** — a single shared token in the
 * `x-admin-token` header (config: overseas.admin-token).
 * tjmall's admin side uses full JWT login (tjMall-admin); for this
 * two-endpoint admin surface the token keeps the same header-check shape
 * without dragging in the whole auth stack. Swap for JWT when a real
 * overseas admin console is built.
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    @Value("${overseas.admin-token:}")
    private String adminToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("x-admin-token");
        if (StrUtil.isBlank(adminToken) || !adminToken.equals(token)) {
            throw new OverseasException(OverseasResultCode.ADMIN_TOKEN_INVALID);
        }
        return true;
    }
}
