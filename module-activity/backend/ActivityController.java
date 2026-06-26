package com.campus.team.api;

import com.campus.team.api.dto.ActivitySaveRequest;
import com.campus.team.api.dto.ActivitySearchRequest;
import com.campus.team.api.vo.ActivityVO;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.ActivityService;
import com.campus.team.data.entity.ActivityCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/categories")
    public Result<List<ActivityCategory>> categories() {
        return Result.ok(activityService.listCategories());
    }

    @GetMapping("/activities/search")
    public Result<PageResult<ActivityVO>> search(ActivitySearchRequest request) {
        return Result.ok(activityService.search(request));
    }

    @GetMapping("/activities/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.ok(activityService.getDetail(id));
    }

    @PostMapping("/activities")
    public Result<Map<String, Long>> publish(@Valid @RequestBody ActivitySaveRequest request) {
        Long id = activityService.publish(request);
        return Result.ok(Map.of("id", id));
    }

    @PutMapping("/activities")
    public Result<Void> update(@Valid @RequestBody ActivitySaveRequest request) {
        activityService.update(request);
        return Result.ok();
    }

    @PutMapping("/activities/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        activityService.offline(id);
        return Result.ok();
    }

    @PutMapping("/activities/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        activityService.withdraw(id);
        return Result.ok();
    }

    @PutMapping("/activities/{id}/republish")
    public Result<Void> republish(@PathVariable Long id, @Valid @RequestBody ActivitySaveRequest request) {
        request.setId(id);
        activityService.republish(request);
        return Result.ok();
    }

    @PutMapping("/activities/{id}/recruit-status")
    public Result<Void> recruitStatus(@PathVariable Long id, @RequestParam Integer status) {
        activityService.updateRecruitStatus(id, status);
        return Result.ok();
    }

    @GetMapping("/activities/my-published")
    public Result<PageResult<ActivityVO>> myPublished(ActivitySearchRequest request) {
        request.setCreatorId(com.campus.team.security.UserContext.getUserId());
        return Result.ok(activityService.search(request));
    }
}
