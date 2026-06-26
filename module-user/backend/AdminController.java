package com.campus.team.api;

import com.campus.team.api.dto.AdminLoginRequest;
import com.campus.team.api.dto.UserRoleUpdateRequest;
import com.campus.team.api.vo.AdminUserVO;
import com.campus.team.api.vo.RoleVO;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.ActivityService;
import com.campus.team.core.AdminService;
import com.campus.team.core.ChatService;
import com.campus.team.core.ReviewService;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.entity.UserComplaint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ActivityService activityService;
    private final ReviewService reviewService;
    private final ChatService chatService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request,
                                              HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        return Result.ok(adminService.login(request, ip));
    }

    @GetMapping("/stats")
    public Result<AdminService.StatsVO> stats() {
        return Result.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> users(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listUsers(page, size, keyword));
    }

    @GetMapping("/roles")
    public Result<List<RoleVO>> roles() {
        return Result.ok(adminService.listAssignableRoles());
    }

    @PutMapping("/users/{id}/roles")
    public Result<Void> updateUserRoles(@PathVariable Long id,
                                        @Valid @RequestBody UserRoleUpdateRequest request) {
        adminService.updateUserRoles(id, request);
        return Result.ok();
    }

    @PutMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return Result.ok();
    }

    @PutMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return Result.ok();
    }

    @GetMapping("/activities")
    public Result<PageResult<Activity>> activities(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminService.listAllActivities(page, size));
    }

    @PutMapping("/activities/{id}/offline")
    public Result<Void> offlineActivity(@PathVariable Long id) {
        activityService.adminOffline(id);
        return Result.ok();
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return Result.ok();
    }

    @DeleteMapping("/messages/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        chatService.deleteMessage(id);
        return Result.ok();
    }

    @GetMapping("/complaints")
    public Result<PageResult<UserComplaint>> complaints(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminService.listComplaints(page, size, status));
    }

    @PutMapping("/complaints/{id}/handle")
    public Result<Void> handleComplaint(@PathVariable Long id,
                                         @RequestParam boolean approved,
                                         @RequestParam(required = false) String remark) {
        adminService.handleComplaint(id, approved, remark);
        return Result.ok();
    }
}
