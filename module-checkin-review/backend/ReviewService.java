package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.ReviewRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.enums.ActivityStatus;
import com.campus.team.common.enums.RoleCode;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.entity.ActivityReview;
import com.campus.team.data.mapper.ActivityMapper;
import com.campus.team.data.entity.SysUser;
import com.campus.team.data.mapper.ActivityReviewMapper;
import com.campus.team.infrastructure.FileUrlHelper;
import com.campus.team.infrastructure.SensitiveWordFilter;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ActivityReviewMapper reviewMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final FileUrlHelper fileUrlHelper;

    @Transactional
    public void submit(ReviewRequest req) {
        PermissionChecker.requireMiniProgramUser();
        Activity activity = activityService.getValidActivity(req.getActivityId());
        activityService.assertActivityOperable(activity);

        if (activity.getActivityStatus() != ActivityStatus.FINISHED.getCode()) {
            throw new BusinessException("活动未结束，暂不可评价");
        }

        registrationService.getApprovedRegistration(req.getActivityId(), UserContext.getUserId());
        sensitiveWordFilter.check(req.getContent());

        ActivityReview existing = reviewMapper.selectOne(new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, req.getActivityId())
                .eq(ActivityReview::getUserId, UserContext.getUserId()));
        if (existing != null) {
            throw new BusinessException("您已评价，请勿重复提交");
        }

        ActivityReview review = new ActivityReview();
        review.setActivityId(req.getActivityId());
        review.setUserId(UserContext.getUserId());
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        review.setStatus(1);
        reviewMapper.insert(review);
    }

    public PageResult<ReviewVO> listByActivity(Long activityId, long page, long size) {
        Page<ActivityReview> p = new Page<>(page, size);
        var result = reviewMapper.selectPage(p, new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .eq(ActivityReview::getStatus, 1)
                .orderByDesc(ActivityReview::getCreatedAt));

        List<ReviewVO> vos = result.getRecords().stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setRating(r.getRating());
            vo.setContent(r.getContent());
            vo.setCreatedAt(r.getCreatedAt());
            SysUser user = userService.getById(r.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(fileUrlHelper.toAccessUrl(user.getAvatar()));
            }
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    public PageResult<ReviewVO> myReviews(long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        Page<ActivityReview> p = new Page<>(page, size);
        var result = reviewMapper.selectPage(p, new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getUserId, UserContext.getUserId())
                .eq(ActivityReview::getStatus, 1)
                .orderByDesc(ActivityReview::getCreatedAt));
        List<ReviewVO> vos = result.getRecords().stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setActivityId(r.getActivityId());
            vo.setRating(r.getRating());
            vo.setContent(r.getContent());
            vo.setCreatedAt(r.getCreatedAt());
            Activity activity = activityMapper.selectById(r.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
            }
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    @Transactional
    public void deleteReview(Long id) {
        PermissionChecker.requireAdminRole(RoleCode.CAMPUS_ADMIN.getCode());
        ActivityReview review = reviewMapper.selectById(id);
        if (review != null) {
            review.setStatus(0);
            reviewMapper.updateById(review);
        }
    }

    @Data
    public static class ReviewVO {
        private Long id;
        private Long activityId;
        private String activityTitle;
        private String nickname;
        private String avatar;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
    }
}
