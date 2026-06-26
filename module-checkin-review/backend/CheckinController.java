package com.campus.team.api;

import com.campus.team.api.dto.CheckinRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.CheckinService;
import com.campus.team.data.entity.ActivityCheckin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping
    public Result<Void> checkin(@Valid @RequestBody CheckinRequest request) {
        checkinService.checkin(request);
        return Result.ok();
    }

    @GetMapping("/stats/{activityId}")
    public Result<CheckinService.CheckinStatsVO> stats(@PathVariable Long activityId) {
        return Result.ok(checkinService.stats(activityId));
    }

    @GetMapping("/my/{activityId}")
    public Result<ActivityCheckin> myCheckin(@PathVariable Long activityId) {
        return Result.ok(checkinService.myCheckin(activityId));
    }

    @GetMapping("/my")
    public Result<PageResult<CheckinService.CheckinRecordVO>> myCheckins(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(checkinService.myCheckins(page, size));
    }
}
