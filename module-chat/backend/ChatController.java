package com.campus.team.api;

import com.campus.team.api.dto.ChatSendRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.Result;
import com.campus.team.core.ChatService;
import com.campus.team.data.entity.ChatMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public Result<ChatMessage> send(@Valid @RequestBody ChatSendRequest request) {
        return Result.ok(chatService.send(request));
    }

    @GetMapping("/conversations")
    public Result<PageResult<ChatService.ConversationVO>> conversations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(chatService.myConversations(page, size));
    }

    @GetMapping("/history/{conversationId}")
    public Result<PageResult<ChatMessage>> history(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        return Result.ok(chatService.history(conversationId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(chatService.unreadCount());
    }

    @GetMapping("/conversation")
    public Result<ChatService.ConversationVO> startConversation(
            @RequestParam Long activityId,
            @RequestParam Long peerId) {
        return Result.ok(chatService.startConversation(activityId, peerId));
    }
}
