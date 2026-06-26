package com.campus.team.api;

import com.campus.team.api.dto.RegistrationApplyRequest;
import com.campus.team.api.dto.RegistrationAuditRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody RegistrationApplyRequest request) {
        registrationService.apply(request);
        return Result.ok();
    }

    @PostMapping("/cancel/{activityId}")
    public Result<Void> cancel(@PathVariable Long activityId) {
        registrationService.cancel(activityId);
        return Result.ok();
    }

    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody RegistrationAuditRequest request) {
        registrationService.audit(request);
        return Result.ok();
    }

    @DeleteMapping("/{activityId}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long activityId, @PathVariable Long userId) {
        registrationService.removeMember(activityId, userId);
        return Result.ok();
    }

    @GetMapping("/activity/{activityId}")
    public Result<PageResult<RegistrationService.RegistrationVO>> listByActivity(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(registrationService.listByActivity(activityId, page, size));
    }

    @GetMapping("/my")
    public Result<PageResult<RegistrationService.RegistrationVO>> myRegistrations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(registrationService.myRegistrations(page, size));
    }
}
