package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.CheckinRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.enums.ActivityStatus;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.entity.ActivityCheckin;
import com.campus.team.data.mapper.ActivityMapper;
import com.campus.team.data.entity.ActivityRegistration;
import com.campus.team.data.entity.SysUser;
import com.campus.team.data.mapper.ActivityCheckinMapper;
import com.campus.team.infrastructure.SensitiveWordFilter;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinService {

    private final ActivityCheckinMapper checkinMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Transactional
    public void checkin(CheckinRequest req) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();
        Activity activity = activityService.getValidActivity(req.getActivityId());
        log.info("[打卡] 开始打卡 userId={}, activityId={}, title={}, activityStatus={}, startTime={}, endTime={}, 当前时间={}",
                userId, activity.getId(), activity.getTitle(), activity.getActivityStatus(),
                activity.getStartTime(), activity.getEndTime(), now);
        activityService.assertActivityOperable(activity);

        if (activity.getActivityStatus() != ActivityStatus.ONGOING.getCode()) {
            log.warn("[打卡] 活动状态不允许打卡 activityId={}, activityStatus={}, 期望status={}, startTime={}, endTime={}, 当前时间={}",
                    activity.getId(), activity.getActivityStatus(), ActivityStatus.ONGOING.getCode(),
                    activity.getStartTime(), activity.getEndTime(), now);
            throw new BusinessException("活动未进行中，无法打卡");
        }

        registrationService.getApprovedRegistration(req.getActivityId(), UserContext.getUserId());
        sensitiveWordFilter.check(req.getContent());

        ActivityCheckin existing = checkinMapper.selectOne(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, req.getActivityId())
                .eq(ActivityCheckin::getUserId, UserContext.getUserId()));
        if (existing != null) {
            throw new BusinessException("您已打卡，请勿重复操作");
        }

        ActivityCheckin checkin = new ActivityCheckin();
        checkin.setActivityId(req.getActivityId());
        checkin.setUserId(UserContext.getUserId());
        checkin.setContent(req.getContent());
        checkin.setCheckinTime(now);
        checkinMapper.insert(checkin);
        log.info("[打卡] 打卡成功 userId={}, activityId={}, checkinTime={}", userId, req.getActivityId(), now);
    }

    public CheckinStatsVO stats(Long activityId) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityService.getValidActivity(activityId);
        PermissionChecker.requireCreator(activity.getCreatorId());

        List<ActivityCheckin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activityId));

        CheckinStatsVO stats = new CheckinStatsVO();
        stats.setTotalCheckins(checkins.size());
        stats.setDetails(checkins.stream().map(c -> {
            CheckinDetailVO d = new CheckinDetailVO();
            d.setUserId(c.getUserId());
            d.setCheckinTime(c.getCheckinTime());
            d.setContent(c.getContent());
            SysUser user = userService.getById(c.getUserId());
            if (user != null) {
                d.setNickname(user.getNickname());
            }
            return d;
        }).toList());
        return stats;
    }

    public ActivityCheckin myCheckin(Long activityId) {
        PermissionChecker.requireMiniProgramUser();
        return checkinMapper.selectOne(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activityId)
                .eq(ActivityCheckin::getUserId, UserContext.getUserId()));
    }

    public PageResult<CheckinRecordVO> myCheckins(long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        Page<ActivityCheckin> p = new Page<>(page, size);
        var result = checkinMapper.selectPage(p, new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getUserId, UserContext.getUserId())
                .orderByDesc(ActivityCheckin::getCheckinTime));
        List<CheckinRecordVO> vos = result.getRecords().stream().map(c -> {
            CheckinRecordVO vo = new CheckinRecordVO();
            vo.setId(c.getId());
            vo.setActivityId(c.getActivityId());
            vo.setContent(c.getContent());
            vo.setCheckinTime(c.getCheckinTime());
            Activity activity = activityMapper.selectById(c.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
                vo.setActivityStatus(activity.getActivityStatus());
            }
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    @Data
    public static class CheckinRecordVO {
        private Long id;
        private Long activityId;
        private String activityTitle;
        private Integer activityStatus;
        private String content;
        private LocalDateTime checkinTime;
    }

    @Data
    public static class CheckinStatsVO {
        private int totalCheckins;
        private List<CheckinDetailVO> details;
    }

    @Data
    public static class CheckinDetailVO {
        private Long userId;
        private String nickname;
        private LocalDateTime checkinTime;
        private String content;
    }
}
