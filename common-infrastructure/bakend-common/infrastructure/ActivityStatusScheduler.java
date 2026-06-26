package com.campus.team.infrastructure;

import com.campus.team.common.enums.ActivityStatus;
import com.campus.team.common.enums.RecruitStatus;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityStatusScheduler {

    private final ActivityMapper activityMapper;

    @Scheduled(cron = "0 */5 * * * ?")
    public void refreshActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        List<Activity> activities = activityMapper.selectList(null);
        for (Activity activity : activities) {
            if (activity.getActivityStatus() == ActivityStatus.OFFLINE.getCode()) {
                continue;
            }
            int newStatus = activity.getActivityStatus();
            if (now.isBefore(activity.getStartTime())) {
                newStatus = ActivityStatus.RECRUITING.getCode();
            } else if (!now.isAfter(activity.getEndTime())) {
                newStatus = ActivityStatus.ONGOING.getCode();
            } else {
                newStatus = ActivityStatus.FINISHED.getCode();
            }
            boolean changed = false;
            if (newStatus != activity.getActivityStatus()) {
                log.info("[活动状态调度] activityId={}, title={}, 原status={}, 新status={}, startTime={}, endTime={}, 当前时间={}",
                        activity.getId(), activity.getTitle(), activity.getActivityStatus(), newStatus,
                        activity.getStartTime(), activity.getEndTime(), now);
                activity.setActivityStatus(newStatus);
                changed = true;
            }
            if (activity.getRecruitStatus() == RecruitStatus.OPEN.getCode()
                    && now.isAfter(activity.getRegisterDeadline())) {
                activity.setRecruitStatus(RecruitStatus.STOPPED.getCode());
                changed = true;
            }
            if (changed) {
                activityMapper.updateById(activity);
            }
        }
    }
}
