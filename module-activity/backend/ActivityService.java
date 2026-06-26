package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.ActivitySaveRequest;
import com.campus.team.api.dto.ActivitySearchRequest;
import com.campus.team.api.vo.ActivityVO;
import com.campus.team.common.PageResult;
import com.campus.team.common.enums.ActivityStatus;
import com.campus.team.common.enums.RecruitStatus;
import com.campus.team.common.enums.RegistrationStatus;
import com.campus.team.common.enums.RoleCode;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.entity.ActivityCategory;
import com.campus.team.data.entity.ActivityRegistration;
import com.campus.team.data.entity.SysUser;
import com.campus.team.data.mapper.ActivityCategoryMapper;
import com.campus.team.data.mapper.ActivityMapper;
import com.campus.team.data.entity.ActivityCheckin;
import com.campus.team.data.entity.ActivityReview;
import com.campus.team.data.mapper.ActivityCheckinMapper;
import com.campus.team.data.mapper.ActivityRegistrationMapper;
import com.campus.team.data.mapper.ActivityReviewMapper;
import com.campus.team.infrastructure.FileUrlHelper;
import com.campus.team.infrastructure.SensitiveWordFilter;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityCheckinMapper checkinMapper;
    private final ActivityReviewMapper reviewMapper;
    private final GroupChatService groupChatService;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final FileUrlHelper fileUrlHelper;

    public List<ActivityCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ActivityCategory>()
                .eq(ActivityCategory::getStatus, 1)
                .orderByAsc(ActivityCategory::getSortOrder));
    }

    public PageResult<ActivityVO> search(ActivitySearchRequest req) {
        Page<Activity> page = new Page<>(req.getPage(), req.getSize());
        var result = activityMapper.searchActivities(page, req.getKeyword(), req.getCategoryId(),
                req.getActivityStatus(), req.getCreatorId());
        List<ActivityVO> vos = result.getRecords().stream().map(this::toVO).toList();
        fillExtraInfo(vos);
        return PageResult.of(result.getTotal(), req.getPage(), req.getSize(), vos);
    }

    public ActivityVO getDetail(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getActivityStatus() == ActivityStatus.OFFLINE.getCode()) {
            Long userId = UserContext.getUserId();
            if (userId == null || !userId.equals(activity.getCreatorId())) {
                throw new BusinessException("活动已下架");
            }
        }
        ActivityVO vo = toVO(activity);
        fillExtraInfo(List.of(vo));
        return vo;
    }

    @Transactional
    public Long publish(ActivitySaveRequest req) {
        PermissionChecker.requireCreatorRole();
        userService.requireProfileCompleted(UserContext.getUserId());
        validateTime(req);

        sensitiveWordFilter.check(req.getTitle());
        sensitiveWordFilter.check(req.getDescription());
        sensitiveWordFilter.check(req.getLocation());

        Activity activity = new Activity();
        activity.setCreatorId(UserContext.getUserId());
        fillActivity(activity, req);
        activity.setCurrentMembers(0);
        initRecruitStatus(activity);
        activity.setActivityStatus(ActivityStatus.RECRUITING.getCode());
        activityMapper.insert(activity);
        return activity.getId();
    }

    @Transactional
    public void update(ActivitySaveRequest req) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(req.getId());
        if (activity == null || activity.getActivityStatus() == ActivityStatus.OFFLINE.getCode()) {
            throw new BusinessException("活动不存在或已下架");
        }
        if (activity.getActivityStatus() == ActivityStatus.FINISHED.getCode()) {
            throw new BusinessException("已结束活动不可编辑");
        }
        PermissionChecker.requireCreator(activity.getCreatorId());
        validateTimeForUpdate(activity, req);
        sensitiveWordFilter.check(req.getTitle());
        sensitiveWordFilter.check(req.getDescription());
        fillActivity(activity, req);
        syncRecruitStatusByDeadline(activity);
        activityMapper.updateById(activity);
    }

    @Transactional
    public void offline(Long id) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        PermissionChecker.requireCreator(activity.getCreatorId());
        activity.setActivityStatus(ActivityStatus.OFFLINE.getCode());
        activityMapper.updateById(activity);
    }

    @Transactional
    public void withdraw(Long id) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        PermissionChecker.requireCreator(activity.getCreatorId());
        if (activity.getActivityStatus() != ActivityStatus.RECRUITING.getCode()) {
            throw new BusinessException("仅招募中的活动可撤回");
        }
        activity.setActivityStatus(ActivityStatus.OFFLINE.getCode());
        activityMapper.updateById(activity);
    }

    @Transactional
    public void republish(ActivitySaveRequest req) {
        PermissionChecker.requireCreatorRole();
        userService.requireProfileCompleted(UserContext.getUserId());
        if (req.getId() == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Activity activity = activityMapper.selectById(req.getId());
        if (activity == null || activity.getActivityStatus() != ActivityStatus.OFFLINE.getCode()) {
            throw new BusinessException("活动不存在或状态不允许重新发布");
        }
        PermissionChecker.requireCreator(activity.getCreatorId());
        validateTime(req);
        sensitiveWordFilter.check(req.getTitle());
        sensitiveWordFilter.check(req.getDescription());
        sensitiveWordFilter.check(req.getLocation());

        clearRegistrationsForRepublish(activity.getId());

        fillActivity(activity, req);
        activity.setCurrentMembers(0);
        initRecruitStatus(activity);
        activity.setActivityStatus(ActivityStatus.RECRUITING.getCode());
        activityMapper.updateById(activity);
    }

    private void clearRegistrationsForRepublish(Long activityId) {
        List<ActivityRegistration> regs = registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .in(ActivityRegistration::getStatus,
                        RegistrationStatus.PENDING.getCode(),
                        RegistrationStatus.APPROVED.getCode()));
        for (ActivityRegistration reg : regs) {
            reg.setStatus(RegistrationStatus.CANCELLED.getCode());
            registrationMapper.updateById(reg);
        }
    }

    @Transactional
    public void adminOffline(Long id) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        activity.setActivityStatus(ActivityStatus.OFFLINE.getCode());
        activityMapper.updateById(activity);
    }

    @Transactional
    public void updateRecruitStatus(Long id, Integer recruitStatus) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(id);
        PermissionChecker.requireCreator(activity.getCreatorId());
        if (activity.getActivityStatus() == ActivityStatus.OFFLINE.getCode()) {
            throw new BusinessException("活动已下架，无法调整招募状态");
        }
        activity.setRecruitStatus(recruitStatus);
        activityMapper.updateById(activity);
    }

    public Activity getValidActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getActivityStatus() == ActivityStatus.OFFLINE.getCode()) {
            throw new BusinessException("活动已下架");
        }
        return activity;
    }

    public void assertActivityOperable(Activity activity) {
        if (activity.getActivityStatus() == ActivityStatus.OFFLINE.getCode()) {
            throw new BusinessException("活动已下架，相关操作已禁止");
        }
    }

    private void validateTime(ActivitySaveRequest req) {
        LocalDateTime now = LocalDateTime.now();
        if (!req.getStartTime().isAfter(now)) {
            throw new BusinessException("活动开始时间须晚于当前时间");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (req.getRegisterDeadline().isAfter(req.getStartTime())) {
            throw new BusinessException("报名截止时间不能晚于活动开始时间");
        }
        if (req.getMaxMembers() < 0) {
            throw new BusinessException("人数上限不合法");
        }
    }

    private void validateTimeForUpdate(Activity activity, ActivitySaveRequest req) {
        LocalDateTime now = LocalDateTime.now();
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (req.getRegisterDeadline().isAfter(req.getStartTime())) {
            throw new BusinessException("报名截止时间不能晚于活动开始时间");
        }
        if (req.getMaxMembers() < 0) {
            throw new BusinessException("人数上限不合法");
        }
        if (req.getMaxMembers() > 0 && req.getMaxMembers() < activity.getCurrentMembers()) {
            throw new BusinessException("人数上限不能小于当前已通过人数");
        }
        if (activity.getActivityStatus() == ActivityStatus.RECRUITING.getCode()) {
            if (!req.getStartTime().isAfter(now)) {
                throw new BusinessException("活动开始时间须晚于当前时间");
            }
        } else if (activity.getActivityStatus() == ActivityStatus.ONGOING.getCode()) {
            if (!req.getEndTime().isAfter(now)) {
                throw new BusinessException("结束时间须晚于当前时间");
            }
        }
    }

    private void fillActivity(Activity activity, ActivitySaveRequest req) {
        activity.setCategoryId(req.getCategoryId());
        activity.setTitle(req.getTitle());
        activity.setDescription(req.getDescription());
        activity.setCoverImage(fileUrlHelper.toStoredPath(req.getCoverImage()));
        activity.setLocation(req.getLocation());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setRegisterDeadline(req.getRegisterDeadline());
        activity.setMaxMembers(req.getMaxMembers());
        activity.setTags(req.getTags());
    }

    private void initRecruitStatus(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getRegisterDeadline())) {
            activity.setRecruitStatus(RecruitStatus.STOPPED.getCode());
        } else {
            activity.setRecruitStatus(RecruitStatus.OPEN.getCode());
        }
    }

    private void syncRecruitStatusByDeadline(Activity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getRegisterDeadline())) {
            if (activity.getRecruitStatus() == RecruitStatus.OPEN.getCode()) {
                activity.setRecruitStatus(RecruitStatus.STOPPED.getCode());
            }
        } else if (activity.getRecruitStatus() == RecruitStatus.STOPPED.getCode()) {
            if (activity.getMaxMembers() > 0 && activity.getCurrentMembers() >= activity.getMaxMembers()) {
                activity.setRecruitStatus(RecruitStatus.FULL.getCode());
            } else {
                activity.setRecruitStatus(RecruitStatus.OPEN.getCode());
            }
        }
    }

    private ActivityVO toVO(Activity activity) {
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setCreatorId(activity.getCreatorId());
        vo.setCategoryId(activity.getCategoryId());
        vo.setTitle(activity.getTitle());
        vo.setDescription(activity.getDescription());
        vo.setCoverImage(fileUrlHelper.toAccessUrl(activity.getCoverImage()));
        vo.setLocation(activity.getLocation());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setRegisterDeadline(activity.getRegisterDeadline());
        vo.setMaxMembers(activity.getMaxMembers());
        vo.setCurrentMembers(activity.getCurrentMembers());
        vo.setRecruitStatus(activity.getRecruitStatus());
        vo.setActivityStatus(activity.getActivityStatus());
        vo.setTags(activity.getTags());
        vo.setCreatedAt(activity.getCreatedAt());
        vo.setRegisterClosed(LocalDateTime.now().isAfter(activity.getRegisterDeadline()));

        ActivityCategory cat = categoryMapper.selectById(activity.getCategoryId());
        if (cat != null) {
            vo.setCategoryName(cat.getName());
        }
        SysUser creator = userService.getById(activity.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getNickname());
        }
        return vo;
    }

    private void fillExtraInfo(List<ActivityVO> vos) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return;
        }
        List<Long> ids = vos.stream().map(ActivityVO::getId).toList();
        if (ids.isEmpty()) {
            return;
        }
        List<ActivityRegistration> regs = registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getUserId, userId)
                .in(ActivityRegistration::getActivityId, ids));
        Map<Long, Integer> statusMap = regs.stream()
                .collect(Collectors.toMap(ActivityRegistration::getActivityId, ActivityRegistration::getStatus, (a, b) -> a));

        List<ActivityCheckin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getUserId, userId)
                .in(ActivityCheckin::getActivityId, ids));
        Map<Long, ActivityCheckin> checkinMap = checkins.stream()
                .collect(Collectors.toMap(ActivityCheckin::getActivityId, c -> c, (a, b) -> a));

        List<ActivityReview> reviews = reviewMapper.selectList(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getUserId, userId)
                .eq(ActivityReview::getStatus, 1)
                .in(ActivityReview::getActivityId, ids));
        Map<Long, ActivityReview> reviewMap = reviews.stream()
                .collect(Collectors.toMap(ActivityReview::getActivityId, r -> r, (a, b) -> a));

        for (ActivityVO vo : vos) {
            vo.setMyRegistrationStatus(statusMap.get(vo.getId()));
            vo.setIsCreator(userId.equals(vo.getCreatorId()));
            vo.setMyHasCheckin(checkinMap.containsKey(vo.getId()));
            vo.setMyHasReview(reviewMap.containsKey(vo.getId()));
            vo.setGroupId(groupChatService.getGroupIdForUser(vo.getId(), userId));
            vo.setInGroup(vo.getGroupId() != null);
        }
    }
}
