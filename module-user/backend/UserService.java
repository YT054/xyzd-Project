package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.team.api.dto.ProfileUpdateRequest;
import com.campus.team.api.dto.UserLoginRequest;
import com.campus.team.api.dto.UserRegisterRequest;
import com.campus.team.api.dto.WxLoginRequest;
import com.campus.team.api.vo.LoginVO;
import com.campus.team.api.vo.UserProfileVO;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.SysUser;
import com.campus.team.data.mapper.ChatMessageMapper;
import com.campus.team.data.mapper.SysUserMapper;
import com.campus.team.infrastructure.FileUrlHelper;
import com.campus.team.infrastructure.SensitiveWordFilter;
import com.campus.team.infrastructure.WeChatService;
import com.campus.team.security.JwtUtil;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final WeChatService weChatService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final JwtUtil jwtUtil;
    private final FileUrlHelper fileUrlHelper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public LoginVO register(UserRegisterRequest request) {
        sensitiveWordFilter.check(request.getUsername());
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            sensitiveWordFilter.check(request.getNickname());
        }

        Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists > 0) {
            throw new BusinessException("账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname() : request.getUsername());
        user.setProfileCompleted(0);
        user.setStatus(1);
        sysUserMapper.insert(user);
        return buildLoginVO(user);
    }

    public LoginVO accountLogin(UserLoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null) {
            log.warn("[用户登录] 账号不存在, username={}", request.getUsername());
            throw new BusinessException("账号或密码错误");
        }

        String inputEncoded = passwordEncoder.encode(request.getPassword());
        boolean matched = user.getPassword() != null && !user.getPassword().isBlank()
                && passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("[用户登录] username={}, 输入密码明文={}, 输入密码BCrypt={}, 数据库BCrypt={}, 校验结果={}",
                request.getUsername(), request.getPassword(), inputEncoded, user.getPassword(), matched);

        if (!matched) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        return buildLoginVO(user);
    }

    @Transactional
    public LoginVO wxLogin(WxLoginRequest request) {
        Map<String, String> session = weChatService.code2Session(request.getCode());
        String openid = session.get("openid");

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenid, openid));
        if (user == null) {
            user = new SysUser();
            user.setOpenid(openid);
            user.setUnionId(session.get("unionid"));
            user.setNickname("新用户");
            user.setProfileCompleted(0);
            user.setStatus(1);
            sysUserMapper.insert(user);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        return buildLoginVO(user);
    }

    private LoginVO buildLoginVO(SysUser user) {
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setProfileCompleted(user.getProfileCompleted() == 1);
        vo.setRoles(sysUserMapper.selectRoleCodesByUserId(user.getId()));
        vo.setToken(jwtUtil.createUserToken(user.getId(), user.getOpenid()));
        return vo;
    }

    public UserProfileVO getProfile() {
        PermissionChecker.requireLogin();
        SysUser user = sysUserMapper.selectById(UserContext.getUserId());
        UserProfileVO vo = toProfileVO(user);
        vo.setUnreadMessages(chatMessageMapper.countUnread(user.getId()));
        return vo;
    }

    @Transactional
    public UserProfileVO updateProfile(ProfileUpdateRequest request) {
        PermissionChecker.requireLogin();
        sensitiveWordFilter.check(request.getNickname());
        sensitiveWordFilter.check(request.getStudentNo());
        sensitiveWordFilter.check(request.getCollege());

        SysUser user = sysUserMapper.selectById(UserContext.getUserId());
        user.setNickname(request.getNickname());
        user.setAvatar(fileUrlHelper.toStoredPath(request.getAvatar()));
        user.setPhone(request.getPhone());
        user.setStudentNo(request.getStudentNo());
        user.setCollege(request.getCollege());
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getPrivacyLevel() != null) {
            user.setPrivacyLevel(request.getPrivacyLevel());
        }
        if (isProfileComplete(user)) {
            user.setProfileCompleted(1);
        }
        sysUserMapper.updateById(user);
        return toProfileVO(user);
    }

    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    private boolean isProfileComplete(SysUser user) {
        return user.getNickname() != null && !user.getNickname().isBlank()
                && user.getStudentNo() != null && !user.getStudentNo().isBlank()
                && user.getCollege() != null && !user.getCollege().isBlank();
    }

    private UserProfileVO toProfileVO(SysUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(fileUrlHelper.toAccessUrl(user.getAvatar()));
        vo.setPhone(user.getPhone());
        vo.setStudentNo(user.getStudentNo());
        vo.setCollege(user.getCollege());
        vo.setGender(user.getGender());
        vo.setPrivacyLevel(user.getPrivacyLevel());
        vo.setProfileCompleted(user.getProfileCompleted());
        vo.setRoles(sysUserMapper.selectRoleCodesByUserId(user.getId()));
        return vo;
    }

    public void requireProfileCompleted(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getProfileCompleted() != 1) {
            throw new BusinessException(403, "请先完善个人资料");
        }
    }
}
