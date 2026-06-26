package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.RegistrationApplyRequest;
import com.campus.team.api.dto.RegistrationAuditRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.enums.ActivityStatus;
import com.campus.team.common.enums.RecruitStatus;
import com.campus.team.common.enums.RegistrationStatus;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.entity.ActivityRegistration;
import com.campus.team.data.entity.SysUser;
import com.campus.team.data.mapper.ActivityMapper;
import com.campus.team.data.mapper.ActivityRegistrationMapper;
import com.campus.team.infrastructure.FileUrlHelper;
import com.campus.team.infrastructure.SensitiveWordFilter;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import com.campus.team.core.event.RegistrationApprovedEvent;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final FileUrlHelper fileUrlHelper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void apply(RegistrationApplyRequest req) {
        PermissionChecker.requireMiniProgramUser();
        userService.requireProfileCompleted(UserContext.getUserId());

        Activity activity = activityService.getValidActivity(req.getActivityId());
        activityService.assertActivityOperable(activity);

        if (LocalDateTime.now().isAfter(activity.getRegisterDeadline())) {
            throw new BusinessException("报名已截止");
        }
        if (activity.getRecruitStatus() != RecruitStatus.OPEN.getCode()) {
            throw new BusinessException("当前不在招募中");
        }
        if (activity.getMaxMembers() > 0 && activity.getCurrentMembers() >= activity.getMaxMembers()) {
            throw new BusinessException("活动已满员");
        }
        if (activity.getCreatorId().equals(UserContext.getUserId())) {
            throw new BusinessException("不能报名自己发布的活动");
        }

        ActivityRegistration existing = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, req.getActivityId())
                .eq(ActivityRegistration::getUserId, UserContext.getUserId()));
        if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED.getCode()
                && existing.getStatus() != RegistrationStatus.REJECTED.getCode()) {
            throw new BusinessException("请勿重复报名");
        }

        sensitiveWordFilter.check(req.getApplyMessage());

        ActivityRegistration reg = existing != null ? existing : new ActivityRegistration();
        reg.setActivityId(req.getActivityId());
        reg.setUserId(UserContext.getUserId());
        reg.setStatus(RegistrationStatus.PENDING.getCode());
        reg.setApplyMessage(req.getApplyMessage());
        reg.setAuditMessage(null);
        reg.setAuditedAt(null);
        if (existing != null) {
            registrationMapper.updateById(reg);
        } else {
            registrationMapper.insert(reg);
        }
    }

    @Transactional
    public void cancel(Long activityId) {
        PermissionChecker.requireMiniProgramUser();
        Activity activity = activityService.getValidActivity(activityId);
        if (!LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new BusinessException("活动已开始，无法取消报名");
        }

        ActivityRegistration reg = getUserRegistration(activityId, UserContext.getUserId());
        if (reg.getStatus() == RegistrationStatus.CANCELLED.getCode()) {
            throw new BusinessException("已取消报名");
        }
        if (reg.getStatus() == RegistrationStatus.APPROVED.getCode()) {
            activity.setCurrentMembers(Math.max(0, activity.getCurrentMembers() - 1));
            if (activity.getRecruitStatus() == RecruitStatus.FULL.getCode()) {
                activity.setRecruitStatus(RecruitStatus.OPEN.getCode());
            }
            activityMapper.updateById(activity);
        }
        reg.setStatus(RegistrationStatus.CANCELLED.getCode());
        registrationMapper.updateById(reg);
    }

    @Transactional
    public void audit(RegistrationAuditRequest req) {
        PermissionChecker.requireCreatorRole();
        ActivityRegistration reg = registrationMapper.selectById(req.getRegistrationId());
        if (reg == null) {
            throw new BusinessException("报名记录不存在");
        }
        Activity activity = activityMapper.selectById(reg.getActivityId());
        PermissionChecker.requireCreator(activity.getCreatorId());
        activityService.assertActivityOperable(activity);

        if (reg.getStatus() != RegistrationStatus.PENDING.getCode()) {
            throw new BusinessException("该报名已处理");
        }

        reg.setAuditMessage(req.getAuditMessage());
        reg.setAuditedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(req.getApproved())) {
            if (activity.getMaxMembers() > 0 && activity.getCurrentMembers() >= activity.getMaxMembers()) {
                throw new BusinessException("活动已满员，无法通过");
            }
            reg.setStatus(RegistrationStatus.APPROVED.getCode());
            activity.setCurrentMembers(activity.getCurrentMembers() + 1);
            if (activity.getMaxMembers() > 0 && activity.getCurrentMembers() >= activity.getMaxMembers()) {
                activity.setRecruitStatus(RecruitStatus.FULL.getCode());
            }
            eventPublisher.publishEvent(new RegistrationApprovedEvent(
                    activity.getId(), reg.getUserId(), activity.getTitle(), activity.getCreatorId()));
        } else {
            reg.setStatus(RegistrationStatus.REJECTED.getCode());
        }
        registrationMapper.updateById(reg);
        activityMapper.updateById(activity);
    }

    @Transactional
    public void removeMember(Long activityId, Long userId) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(activityId);
        PermissionChecker.requireCreator(activity.getCreatorId());

        ActivityRegistration reg = getUserRegistration(activityId, userId);
        if (reg.getStatus() != RegistrationStatus.APPROVED.getCode()) {
            throw new BusinessException("该用户未通过审核");
        }
        reg.setStatus(RegistrationStatus.CANCELLED.getCode());
        registrationMapper.updateById(reg);
        activity.setCurrentMembers(Math.max(0, activity.getCurrentMembers() - 1));
        if (activity.getRecruitStatus() == RecruitStatus.FULL.getCode()) {
            activity.setRecruitStatus(RecruitStatus.OPEN.getCode());
        }
        activityMapper.updateById(activity);
    }

    public PageResult<RegistrationVO> listByActivity(Long activityId, long page, long size) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(activityId);
        PermissionChecker.requireCreator(activity.getCreatorId());

        Page<ActivityRegistration> p = new Page<>(page, size);
        var result = registrationMapper.selectPage(p, new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .orderByDesc(ActivityRegistration::getCreatedAt));

        List<RegistrationVO> vos = result.getRecords().stream().map(r -> {
            RegistrationVO vo = new RegistrationVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setStatus(r.getStatus());
            vo.setApplyMessage(r.getApplyMessage());
            vo.setAuditMessage(r.getAuditMessage());
            vo.setCreatedAt(r.getCreatedAt());
            SysUser user = userService.getById(r.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(fileUrlHelper.toAccessUrl(user.getAvatar()));
                vo.setCollege(user.getCollege());
            }
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    public PageResult<RegistrationVO> myRegistrations(long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        log.info("[打卡排查] 查询我的报名 userId={}, page={}, size={}, 当前时间={}", userId, page, size, now);

        Page<ActivityRegistration> p = new Page<>(page, size);
        var result = registrationMapper.selectPage(p, new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getUserId, userId)
                .orderByDesc(ActivityRegistration::getCreatedAt));
        List<RegistrationVO> vos = result.getRecords().stream().map(r -> {
            RegistrationVO vo = new RegistrationVO();
            vo.setId(r.getId());
            vo.setActivityId(r.getActivityId());
            vo.setStatus(r.getStatus());
            vo.setCreatedAt(r.getCreatedAt());
            Activity activity = activityMapper.selectById(r.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
                vo.setActivityStatus(activity.getActivityStatus());
                vo.setStartTime(activity.getStartTime());
                boolean approved = r.getStatus() == RegistrationStatus.APPROVED.getCode();
                boolean ongoing = activity.getActivityStatus() == ActivityStatus.ONGOING.getCode();
                boolean startedByTime = !now.isBefore(activity.getStartTime());
                boolean notEndedByTime = !now.isAfter(activity.getEndTime());
                log.info("[打卡排查] registrationId={}, activityId={}, title={}, 报名status={}, activityStatus={}, startTime={}, endTime={}, 当前时间={}, 已通过报名={}, 状态为进行中(2)={}, 按开始时间已开始={}, 按结束时间未结束={}, 前端可打卡={}",
                        r.getId(), activity.getId(), activity.getTitle(), r.getStatus(), activity.getActivityStatus(),
                        activity.getStartTime(), activity.getEndTime(), now, approved, ongoing, startedByTime, notEndedByTime,
                        approved && ongoing);
            } else {
                log.warn("[打卡排查] registrationId={}, activityId={} 对应活动不存在", r.getId(), r.getActivityId());
            }
            return vo;
        }).toList();
        long checkinEligible = vos.stream()
                .filter(vo -> vo.getStatus() == RegistrationStatus.APPROVED.getCode()
                        && vo.getActivityStatus() != null
                        && vo.getActivityStatus() == ActivityStatus.ONGOING.getCode())
                .count();
        log.info("[打卡排查] 本次返回报名数={}, 可打卡活动数={}", vos.size(), checkinEligible);
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    public boolean isApprovedParticipant(Long activityId, Long userId) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, RegistrationStatus.APPROVED.getCode()));
        return reg != null;
    }

    public ActivityRegistration getApprovedRegistration(Long activityId, Long userId) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, RegistrationStatus.APPROVED.getCode()));
        if (reg == null) {
            throw new BusinessException("您尚未通过该活动报名审核");
        }
        return reg;
    }

    private ActivityRegistration getUserRegistration(Long activityId, Long userId) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (reg == null) {
            throw new BusinessException("未找到报名记录");
        }
        return reg;
    }

    @Data
    public static class RegistrationVO {
        private Long id;
        private Long activityId;
        private String activityTitle;
        private Integer activityStatus;
        private LocalDateTime startTime;
        private Long userId;
        private String nickname;
        private String avatar;
        private String college;
        private Integer status;
        private String applyMessage;
        private String auditMessage;
        private LocalDateTime createdAt;
    }
}
