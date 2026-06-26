package com.campus.team.security;

import com.campus.team.common.enums.RoleCode;
import com.campus.team.common.exception.BusinessException;

import java.util.Arrays;
import java.util.List;

public final class PermissionChecker {

    private PermissionChecker() {
    }

    public static void requireLogin() {
        if (UserContext.get() == null || UserContext.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    public static void requireMiniProgramUser() {
        requireLogin();
        if (!hasRole(RoleCode.USER.getCode()) && !hasRole(RoleCode.CREATOR.getCode())) {
            throw new BusinessException(403, "未分配用户权限，请联系管理员");
        }
    }

    public static void requireCreatorRole() {
        requireLogin();
        if (!hasRole(RoleCode.CREATOR.getCode())) {
            throw new BusinessException(403, "未分配活动发起者权限，请联系管理员");
        }
    }

    public static void requireProfileCompleted() {
        requireLogin();
    }

    public static void requireCreator(Long creatorId) {
        requireLogin();
        if (!creatorId.equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人活动");
        }
    }

    public static void requireAdminRole(String... roles) {
        LoginUser user = UserContext.get();
        if (user == null || !user.isAdmin()) {
            throw new BusinessException(403, "无管理权限");
        }
        List<String> allowed = Arrays.asList(roles);
        if (!allowed.contains(user.getRoleCode())) {
            throw new BusinessException(403, "角色权限不足");
        }
    }

    public static boolean hasRole(String roleCode) {
        LoginUser user = UserContext.get();
        return user != null && user.getRoles().contains(roleCode);
    }
}
