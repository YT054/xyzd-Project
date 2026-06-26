package com.campus.team.api;

import com.campus.team.api.dto.ReviewRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody ReviewRequest request) {
        reviewService.submit(request);
        return Result.ok();
    }

    @GetMapping("/activity/{activityId}")
    public Result<PageResult<ReviewService.ReviewVO>> listByActivity(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.listByActivity(activityId, page, size));
    }

    @GetMapping("/my")
    public Result<PageResult<ReviewService.ReviewVO>> myReviews(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.myReviews(page, size));
    }
}
