package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.ChatSendRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.data.entity.Activity;
import com.campus.team.data.entity.ChatConversation;
import com.campus.team.data.entity.ChatMessage;
import com.campus.team.data.mapper.ActivityMapper;
import com.campus.team.data.mapper.ChatConversationMapper;
import com.campus.team.data.mapper.ChatMessageMapper;
import com.campus.team.core.event.RegistrationApprovedEvent;
import com.campus.team.infrastructure.FileUrlHelper;
import com.campus.team.infrastructure.RateLimiter;
import com.campus.team.data.entity.SysUser;
import com.campus.team.infrastructure.SensitiveWordFilter;
import com.campus.team.security.PermissionChecker;
import com.campus.team.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final GroupChatService groupChatService;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final RateLimiter rateLimiter;
    private final FileUrlHelper fileUrlHelper;

    @Transactional
    public ChatMessage send(ChatSendRequest req) {
        PermissionChecker.requireMiniProgramUser();
        Long senderId = UserContext.getUserId();
        activityService.getValidActivity(req.getActivityId());
        activityService.assertActivityOperable(activityService.getValidActivity(req.getActivityId()));

        if (senderId.equals(req.getReceiverId())) {
            throw new BusinessException("不能给自己发消息");
        }

        if (!groupChatService.canPrivateMessageInActivity(req.getActivityId(), senderId, req.getReceiverId())
                && !(registrationService.isApprovedParticipant(req.getActivityId(), senderId)
                && registrationService.isApprovedParticipant(req.getActivityId(), req.getReceiverId()))) {
            throw new BusinessException("仅同活动群成员或已通过审核的用户可私信");
        }

        rateLimiter.checkChatRate(senderId);
        sensitiveWordFilter.check(req.getContent());

        ChatConversation conv = getOrCreateConversation(req.getActivityId(), senderId, req.getReceiverId());

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conv.getId());
        msg.setActivityId(req.getActivityId());
        msg.setSenderId(senderId);
        msg.setReceiverId(req.getReceiverId());
        msg.setContent(req.getContent());
        msg.setIsRead(0);
        msg.setStatus(1);
        messageMapper.insert(msg);

        conv.setLastMessage(req.getContent().length() > 100 ? req.getContent().substring(0, 100) : req.getContent());
        conv.setLastMessageTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
        return msg;
    }

    public ConversationVO startConversation(Long activityId, Long peerId) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        activityService.getValidActivity(activityId);

        if (userId.equals(peerId)) {
            throw new BusinessException("不能与自己私信");
        }
        if (!groupChatService.canPrivateMessageInActivity(activityId, userId, peerId)
                && !(registrationService.isApprovedParticipant(activityId, userId)
                && registrationService.isApprovedParticipant(activityId, peerId))) {
            throw new BusinessException("仅同活动群成员或已通过审核的用户可私信");
        }

        ChatConversation conv = getOrCreateConversation(activityId, userId, peerId);
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        vo.setActivityId(activityId);
        vo.setPeerId(peerId);
        SysUser peer = userService.getById(peerId);
        if (peer != null) {
            vo.setPeerName(peer.getNickname());
            vo.setPeerAvatar(fileUrlHelper.toAccessUrl(peer.getAvatar()));
        }
        return vo;
    }

    public PageResult<ConversationVO> myConversations(long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        Page<ChatConversation> p = new Page<>(page, size);
        var result = conversationMapper.selectPage(p, new LambdaQueryWrapper<ChatConversation>()
                .and(w -> w.eq(ChatConversation::getUser1Id, userId).or().eq(ChatConversation::getUser2Id, userId))
                .orderByDesc(ChatConversation::getLastMessageTime));

        List<ConversationVO> vos = result.getRecords().stream().map(c -> {
            ConversationVO vo = new ConversationVO();
            vo.setId(c.getId());
            vo.setActivityId(c.getActivityId());
            vo.setLastMessage(c.getLastMessage());
            vo.setLastMessageTime(c.getLastMessageTime());
            Long peerId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            vo.setPeerId(peerId);
            SysUser peer = userService.getById(peerId);
            if (peer != null) {
                vo.setPeerName(peer.getNickname());
                vo.setPeerAvatar(fileUrlHelper.toAccessUrl(peer.getAvatar()));
            }
            Activity activity = activityMapper.selectById(c.getActivityId());
            if (activity != null) {
                vo.setActivityTitle(activity.getTitle());
            }
            long unread = messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getConversationId, c.getId())
                    .eq(ChatMessage::getReceiverId, userId)
                    .eq(ChatMessage::getIsRead, 0)
                    .eq(ChatMessage::getStatus, 1));
            vo.setUnreadCount(unread);
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    public PageResult<ChatMessage> history(Long conversationId, long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        Long userId = UserContext.getUserId();
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId)) {
            throw new BusinessException(403, "无权查看该会话");
        }

        messageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getReceiverId, userId)
                .eq(ChatMessage::getIsRead, 0)
                .set(ChatMessage::getIsRead, 1));

        Page<ChatMessage> p = new Page<>(page, size);
        var result = messageMapper.selectPage(p, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getStatus, 1)
                .orderByAsc(ChatMessage::getCreatedAt));
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public long unreadCount() {
        PermissionChecker.requireMiniProgramUser();
        return messageMapper.countUnread(UserContext.getUserId());
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        PermissionChecker.requireAdminRole("CAMPUS_ADMIN");
        ChatMessage msg = messageMapper.selectById(messageId);
        if (msg != null) {
            msg.setStatus(0);
            messageMapper.updateById(msg);
        }
    }

    @EventListener
    @Transactional
    public void onRegistrationApproved(RegistrationApprovedEvent event) {
        ChatConversation conv = getOrCreateConversation(event.activityId(), event.creatorId(), event.userId());
        pushNotice(conv, event.activityId(), event.creatorId(), event.userId(),
                "【审核成功】您的报名已通过审核");
        pushNotice(conv, event.activityId(), event.creatorId(), event.userId(),
                "【活动通知】您已成功加入活动《" + event.activityTitle() + "》");
    }

    private void pushNotice(ChatConversation conv, Long activityId, Long senderId, Long receiverId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conv.getId());
        msg.setActivityId(activityId);
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setIsRead(0);
        msg.setStatus(1);
        messageMapper.insert(msg);

        conv.setLastMessage(content.length() > 100 ? content.substring(0, 100) : content);
        conv.setLastMessageTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
    }

    private ChatConversation getOrCreateConversation(Long activityId, Long user1, Long user2) {
        long min = Math.min(user1, user2);
        long max = Math.max(user1, user2);
        ChatConversation conv = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getActivityId, activityId)
                .eq(ChatConversation::getUser1Id, min)
                .eq(ChatConversation::getUser2Id, max));
        if (conv != null) {
            return conv;
        }
        conv = new ChatConversation();
        conv.setActivityId(activityId);
        conv.setUser1Id(min);
        conv.setUser2Id(max);
        conversationMapper.insert(conv);
        return conv;
    }

    @Data
    public static class ConversationVO {
        private Long id;
        private Long activityId;
        private String activityTitle;
        private Long peerId;
        private String peerName;
        private String peerAvatar;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private long unreadCount;
    }
}
