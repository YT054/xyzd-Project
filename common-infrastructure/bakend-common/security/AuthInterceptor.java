package com.campus.team.security;

import com.campus.team.common.exception.BusinessException;
import com.campus.team.config.AdminProperties;
import com.campus.team.data.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final AdminProperties adminProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getServletPath();
        if (isPublicPath(path, request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(token.substring(7));
        } catch (ExpiredJwtException e) {
            throw new BusinessException(401, "令牌已过期，请重新登录");
        } catch (JwtException e) {
            throw new BusinessException(401, "令牌无效，请重新登录");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setTokenType(claims.get("type", String.class));

        if ("admin".equals(loginUser.getTokenType())) {
            checkAdminIp(request);
            loginUser.setAdminId(claims.get("adminId", Long.class));
            loginUser.setUsername(claims.get("username", String.class));
            loginUser.setRoleCode(claims.get("roleCode", String.class));
            loginUser.getRoles().add(loginUser.getRoleCode());
        } else {
            loginUser.setUserId(claims.get("userId", Long.class));
            loginUser.setOpenid(claims.get("openid", String.class));
            List<String> roles = sysUserMapper.selectRoleCodesByUserId(loginUser.getUserId());
            loginUser.setRoles(roles);
        }

        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicPath(String path, String method) {
        if ("GET".equals(method) && (path.contains("/activities/") && path.matches(".*/activities/\\d+$"))) {
            return true;
        }
        if ("GET".equals(method) && path.startsWith("/files/")) {
            return true;
        }
        return path.contains("/auth/wx-login") || path.contains("/auth/login") || path.contains("/auth/register")
                || path.contains("/admin/login")
                || path.contains("/activities/search") || path.endsWith("/categories");
    }

    private void checkAdminIp(HttpServletRequest request) {
        String ip = getClientIp(request);
        List<String> allowed = adminProperties.getAllowedIps();
        if (allowed.isEmpty()) {
            return;
        }
        for (String pattern : allowed) {
            if (pattern.contains("/")) {
                if (ip.startsWith(pattern.split("/")[0].substring(0, pattern.indexOf('.')))) {
                    return;
                }
            } else if (ip.equals(pattern) || "0:0:0:0:0:0:0:1".equals(ip)) {
                return;
            }
        }
        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("127.")) {
            return;
        }
        throw new BusinessException(403, "管理后台仅校园内网IP可访问");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
