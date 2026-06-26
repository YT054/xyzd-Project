package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.AdminLoginRequest;
import com.campus.team.api.dto.ComplaintRequest;
import com.campus.team.api.dto.UserRoleUpdateRequest;
import com.campus.team.api.vo.AdminUserVO;
import com.campus.team.api.vo.RoleVO;
import com.campus.team.common.PageResult;
import com.campus.team.common.enums.RoleCode;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.*;
import com.campus.team.data.mapper.*;
import com.campus.team.security.JwtUtil;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final Set<String> ASSIGNABLE_ROLE_CODES = Set.of(
            RoleCode.USER.getCode(),
            RoleCode.CREATOR.getCode()
    );

    private final SysAdminMapper adminMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final ActivityMapper activityMapper;
    private final ActivityReviewMapper reviewMapper;
    private final UserComplaintMapper complaintMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(AdminLoginRequest req, String ip) {
        SysAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, req.getUsername()));
        if (admin == null || admin.getStatus() != 1) {
            log.warn("[管理员登录] 账号不存在或已禁用, username={}", req.getUsername());
            throw new BusinessException("账号或密码错误");
        }

        String inputEncoded = passwordEncoder.encode(req.getPassword());
        boolean matched = passwordEncoder.matches(req.getPassword(), admin.getPassword());
        boolean admin123Matched = passwordEncoder.matches("admin123", admin.getPassword());
        log.info("[管理员登录] username={}, 输入密码明文={}, 输入密码BCrypt={}, 数据库BCrypt={}, 校验结果={}, admin123与库密文校验={}",
                req.getUsername(), req.getPassword(), inputEncoded, admin.getPassword(), matched, admin123Matched);

        if (!matched) {
            throw new BusinessException("账号或密码错误");
        }

        String roleCode = adminMapper.selectRoleCodeByAdminId(admin.getId());
        admin.setLastLoginIp(ip);
        adminMapper.updateById(admin);

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtUtil.createAdminToken(admin.getId(), admin.getUsername(), roleCode));
        result.put("username", admin.getUsername());
        result.put("realName", admin.getRealName());
        result.put("roleCode", roleCode);
        return result;
    }

    public PageResult<AdminUserVO> listUsers(long page, long size, String keyword) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getStudentNo, keyword));
        }
        var result = userMapper.selectPage(p, wrapper);
        List<AdminUserVO> records = result.getRecords().stream().map(this::toAdminUserVO).toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    public List<RoleVO> listAssignableRoles() {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getRoleCode, ASSIGNABLE_ROLE_CODES)
                        .orderByAsc(SysRole::getId))
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    @Transactional
    public void updateUserRoles(Long userId, UserRoleUpdateRequest req) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        List<String> roleCodes = req.getRoleCodes() == null ? List.of() : req.getRoleCodes();
        for (String roleCode : roleCodes) {
            if (!ASSIGNABLE_ROLE_CODES.contains(roleCode)) {
                throw new BusinessException("不支持分配角色: " + roleCode);
            }
        }

        List<SysRole> roles = roleCodes.isEmpty() ? List.of() : roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, roleCodes));
        if (roles.size() != roleCodes.stream().distinct().count()) {
            throw new BusinessException("存在无效角色");
        }

        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (SysRole role : roles) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            userRoleMapper.insert(ur);
        }
    }

    private AdminUserVO toAdminUserVO(SysUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setStudentNo(user.getStudentNo());
        vo.setCollege(user.getCollege());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setProfileCompleted(user.getProfileCompleted());
        vo.setRoles(userMapper.selectRoleCodesByUserId(user.getId()));
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        return vo;
    }

    @Transactional
    public void disableUser(Long userId) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已是禁用状态");
        }
        user.setStatus(0);
        userMapper.updateById(user);
    }

    @Transactional
    public void enableUser(Long userId) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 1) {
            throw new BusinessException("用户已是正常状态");
        }
        user.setStatus(1);
        userMapper.updateById(user);
    }

    public PageResult<Activity> listAllActivities(long page, long size) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        Page<Activity> p = new Page<>(page, size);
        var result = activityMapper.selectPage(p, new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getCreatedAt));
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    public void submitComplaint(ComplaintRequest req) {
        PermissionChecker.requireMiniProgramUser();
        UserComplaint complaint = new UserComplaint();
        complaint.setReporterId(UserContext.getUserId());
        complaint.setTargetType(req.getTargetType());
        complaint.setTargetId(req.getTargetId());
        complaint.setReason(req.getReason());
        complaint.setStatus(0);
        complaintMapper.insert(complaint);
    }

    public PageResult<UserComplaint> listComplaints(long page, long size, Integer status) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        Page<UserComplaint> p = new Page<>(page, size);
        LambdaQueryWrapper<UserComplaint> wrapper = new LambdaQueryWrapper<UserComplaint>()
                .orderByDesc(UserComplaint::getCreatedAt);
        if (status != null) {
            wrapper.eq(UserComplaint::getStatus, status);
        }
        var result = complaintMapper.selectPage(p, wrapper);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    public void handleComplaint(Long id, boolean approved, String remark) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        UserComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BusinessException("投诉不存在");
        }
        complaint.setStatus(approved ? 1 : 2);
        complaint.setHandlerId(UserContext.getAdminId());
        complaint.setHandleRemark(remark);
        complaint.setHandledAt(LocalDateTime.now());
        complaintMapper.updateById(complaint);
    }

    public StatsVO getStats() {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        StatsVO stats = new StatsVO();
        stats.setUserCount(userMapper.selectCount(null));
        stats.setActivityCount(activityMapper.selectCount(null));
        stats.setReviewCount(reviewMapper.selectCount(new LambdaQueryWrapper<ActivityReview>().eq(ActivityReview::getStatus, 1)));
        stats.setPendingComplaints(complaintMapper.selectCount(new LambdaQueryWrapper<UserComplaint>().eq(UserComplaint::getStatus, 0)));
        return stats;
    }

    @Data
    public static class StatsVO {
        private long userCount;
        private long activityCount;
        private long reviewCount;
        private long pendingComplaints;
    }
}
