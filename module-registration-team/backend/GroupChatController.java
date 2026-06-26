package com.campus.team.api;

import com.campus.team.api.dto.GroupCreateRequest;
import com.campus.team.api.dto.GroupMessageRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.GroupChatService;
import com.campus.team.data.entity.ActivityGroupMessage;
import com.campus.team.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;

    @PostMapping
    public Result<GroupChatService.GroupVO> create(@Valid @RequestBody GroupCreateRequest request) {
        return Result.ok(groupChatService.create(request));
    }

    @GetMapping("/my")
    public Result<PageResult<GroupChatService.GroupVO>> myGroups(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(groupChatService.myGroups(page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(groupChatService.unreadCount());
    }

    @GetMapping("/exists/{activityId}")
    public Result<Boolean> exists(@PathVariable Long activityId) {
        return Result.ok(groupChatService.existsForActivity(activityId));
    }

    @GetMapping("/activity/{activityId}")
    public Result<GroupChatService.GroupVO> byActivity(@PathVariable Long activityId) {
        return Result.ok(groupChatService.getByActivity(activityId));
    }

    @GetMapping("/{id}")
    public Result<GroupChatService.GroupVO> detail(@PathVariable Long id) {
        return Result.ok(groupChatService.getDetail(id));
    }

    @GetMapping("/{id}/members")
    public Result<List<GroupChatService.MemberVO>> members(@PathVariable Long id) {
        return Result.ok(groupChatService.listMembers(id));
    }

    @PostMapping("/{id}/messages")
    public Result<ActivityGroupMessage> send(@PathVariable Long id, @Valid @RequestBody GroupMessageRequest request) {
        return Result.ok(groupChatService.sendMessage(id, request.getContent()));
    }

    @GetMapping("/{id}/messages")
    public Result<PageResult<GroupChatService.GroupMessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        return Result.ok(groupChatService.messages(id, page, size));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        groupChatService.markRead(id, UserContext.getUserId());
        return Result.ok();
    }
}
