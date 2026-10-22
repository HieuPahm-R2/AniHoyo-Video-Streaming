package com.HieuPahm.AniHoyo.config;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.HieuPahm.AniHoyo.model.entities.Permission;
import com.HieuPahm.AniHoyo.model.entities.Role;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.services.implement.UserService;
import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.HieuPahm.AniHoyo.utils.error.ForbidenException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthorityIntercepter implements HandlerInterceptor {
    /**
     * Path the container forwards to after an unhandled exception. This mapping is
     * not an API and therefore never has a permission row; authorising it would
     * replace the original error (5xx/4xx) with a bogus "no permission" response.
     */
    private static final String ERROR_DISPATCH_PATH = "/error";

    @Autowired
    UserService userService;

    @Transactional
    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Interceptors run again for the ERROR (and ASYNC) dispatch of the very same
        // request. Only the original REQUEST dispatch must be authorised; otherwise a
        // failure inside a controller gets re-checked against /error and reported as
        // 403 "you don't have permission", hiding the real exception.
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }

        // Only controller endpoints are described by the permission table. Requests
        // that fall through to a static-resource handler are no API at all and must
        // keep their real 404 (previously they were reported as "no permission").
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();

        if (path == null || ERROR_DISPATCH_PATH.equals(path) || ERROR_DISPATCH_PATH.equals(requestURI)) {
            return true;
        }

        // Check Permission
        String email = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : "";
        if (email != null && !email.isEmpty()) {
            User user = this.userService.handleGetUserByUsername(email);
            if (user == null) {
                log.warn(">>> ACL denied: authenticated principal {} has no matching user record", email);
                throw new ForbidenException("Tài khoản không tồn tại hoặc đã bị xoá");
            }
            Role role = user.getRole();
            if (role == null) {
                log.warn(">>> ACL denied: user {} has no role", email);
                throw new ForbidenException("Tài khoản chưa được gán role nào");
            }
            List<Permission> permissions = role.getPermissions();
            boolean isAccepted = permissions != null && permissions.stream()
                    .anyMatch(item -> Objects.equals(item.getApiPath(), path)
                            && Objects.equals(item.getMethod(), httpMethod));
            if (isAccepted == false) {
                log.warn(">>> ACL denied: role {} has no permission for {} {}", role.getName(), httpMethod, path);
                throw new ForbidenException(
                        "Role '" + role.getName() + "' không có quyền " + httpMethod + " " + path);
            }
        }
        return true;
    }
}
