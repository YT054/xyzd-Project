package com.campus.team.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.api.dto.GroupCreateRequest;
import com.campus.team.common.PageResult;
import com.campus.team.common.enums.RegistrationStatus;
import com.campus.team.common.exception.BusinessException;
import com.campus.team.core.event.RegistrationApprovedEvent;
import com.campus.team.data.entity.*;
import com.campus.team.data.mapper.*;
import com.campus.team.infrastructure.FileUrlHelper;
import com.campus.team.infrastructure.RateLimiter;
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
public class GroupChatService {

    private static final int ROLE_OWNER = 1;
    private static final int MSG_TEXT = 1;
    private static final int MSG_SYSTEM = 2;

    private final ActivityGroupMapper groupMapper;
    private final ActivityGroupMemberMapper memberMapper;
    private final ActivityGroupMessageMapper messageMapper;
    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final UserService userService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final RateLimiter rateLimiter;
    private final FileUrlHelper fileUrlHelper;

    @Transactional
    public GroupVO create(GroupCreateRequest req) {
        PermissionChecker.requireCreatorRole();
        Activity activity = activityMapper.selectById(req.getActivityId());
        if (activity == null || activity.getActivityStatus() == 4) {
            throw new BusinessException("活动不存在或已下架");
        }
        PermissionChecker.requireCreator(activity.getCreatorId());

        ActivityGroup existing = groupMapper.selectOne(new LambdaQueryWrapper<ActivityGroup>()
                .eq(ActivityGroup::getActivityId, req.getActivityId())
                .eq(ActivityGroup::getStatus, 1));
        if (existing != null) {
            throw new BusinessException("该活动已建立群聊");
        }

        ActivityGroup group = new ActivityGroup();
        group.setActivityId(activity.getId());
        group.setName(activity.getTitle() + " 交流群");
        group.setCreatorId(activity.getCreatorId());
        group.setLastMessage("群聊已创建，欢迎交流");
        group.setLastMessageTime(LocalDateTime.now());
        group.setStatus(1);
        groupMapper.insert(group);

        addMemberInternal(group.getId(), activity.getCreatorId(), ROLE_OWNER);
        syncApprovedMembers(group.getId(), activity.getId());

        pushSystemMessage(group, "群聊已创建，已通过审核的成员已自动加入");
        return toGroupVO(group, activity.getCreatorId());
    }

    public GroupVO getByActivity(Long activityId) {
        PermissionChecker.requireMiniProgramUser();
        ActivityGroup group = findActiveGroup(activityId);
        if (group == null) {
            return null;
        }
        Long userId = UserContext.getUserId();
        if (!isMember(group.getId(), userId) && !group.getCreatorId().equals(userId)) {
            return null;
        }
        return toGroupVO(group, userId);
    }

    public GroupVO getDetail(Long groupId) {
        PermissionChecker.requireMiniProgramUser();
        ActivityGroup group = requireGroup(groupId);
        requireMember(groupId, UserContext.getUserId());
        return toGroupVO(group, UserContext.getUserId());
    }

    public PageResult<GroupVO> myGroups(long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        List<ActivityGroupMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getUserId, userId));
        if (memberships.isEmpty()) {
            return PageResult.of(0, page, size, List.of());
        }
        List<Long> groupIds = memberships.stream().map(ActivityGroupMember::getGroupId).toList();
        Page<ActivityGroup> p = new Page<>(page, size);
        var result = groupMapper.selectPage(p, new LambdaQueryWrapper<ActivityGroup>()
                .in(ActivityGroup::getId, groupIds)
                .eq(ActivityGroup::getStatus, 1)
                .orderByDesc(ActivityGroup::getLastMessageTime));
        List<GroupVO> vos = result.getRecords().stream()
                .map(g -> toGroupVO(g, userId))
                .toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    public List<MemberVO> listMembers(Long groupId) {
        PermissionChecker.requireMiniProgramUser();
        requireMember(groupId, UserContext.getUserId());
        List<ActivityGroupMember> members = memberMapper.selectList(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getGroupId, groupId)
                .orderByDesc(ActivityGroupMember::getRole)
                .orderByAsc(ActivityGroupMember::getJoinedAt));
        return members.stream().map(m -> {
            MemberVO vo = new MemberVO();
            vo.setUserId(m.getUserId());
            vo.setRole(m.getRole());
            vo.setJoinedAt(m.getJoinedAt());
            SysUser user = userService.getById(m.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(fileUrlHelper.toAccessUrl(user.getAvatar()));
                vo.setCollege(user.getCollege());
            }
            return vo;
        }).toList();
    }

    @Transactional
    public ActivityGroupMessage sendMessage(Long groupId, String content) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        ActivityGroup group = requireGroup(groupId);
        requireMember(groupId, userId);

        rateLimiter.checkChatRate(userId);
        sensitiveWordFilter.check(content);

        ActivityGroupMessage msg = new ActivityGroupMessage();
        msg.setGroupId(groupId);
        msg.setSenderId(userId);
        msg.setContent(content);
        msg.setMessageType(MSG_TEXT);
        msg.setStatus(1);
        messageMapper.insert(msg);

        group.setLastMessage(content.length() > 100 ? content.substring(0, 100) : content);
        group.setLastMessageTime(LocalDateTime.now());
        groupMapper.updateById(group);
        return msg;
    }

    public PageResult<GroupMessageVO> messages(Long groupId, long page, long size) {
        PermissionChecker.requireMiniProgramUser();
        Long userId = UserContext.getUserId();
        requireMember(groupId, userId);
        markRead(groupId, userId);

        Page<ActivityGroupMessage> p = new Page<>(page, size);
        var result = messageMapper.selectPage(p, new LambdaQueryWrapper<ActivityGroupMessage>()
                .eq(ActivityGroupMessage::getGroupId, groupId)
                .eq(ActivityGroupMessage::getStatus, 1)
                .orderByAsc(ActivityGroupMessage::getCreatedAt));
        List<GroupMessageVO> vos = result.getRecords().stream().map(m -> {
            GroupMessageVO vo = new GroupMessageVO();
            vo.setId(m.getId());
            vo.setGroupId(m.getGroupId());
            vo.setSenderId(m.getSenderId());
            vo.setContent(m.getContent());
            vo.setMessageType(m.getMessageType());
            vo.setCreatedAt(m.getCreatedAt());
            if (m.getMessageType() == MSG_TEXT) {
                SysUser sender = userService.getById(m.getSenderId());
                if (sender != null) {
                    vo.setSenderName(sender.getNickname());
                    vo.setSenderAvatar(fileUrlHelper.toAccessUrl(sender.getAvatar()));
                }
            }
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, vos);
    }

    @Transactional
    public void markRead(Long groupId, Long userId) {
        ActivityGroupMember member = memberMapper.selectOne(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getGroupId, groupId)
                .eq(ActivityGroupMember::getUserId, userId));
        if (member != null) {
            member.setLastReadTime(LocalDateTime.now());
            memberMapper.updateById(member);
        }
    }

    public long unreadCount() {
        PermissionChecker.requireMiniProgramUser();
        return memberMapper.countUnread(UserContext.getUserId());
    }

    public boolean existsForActivity(Long activityId) {
        PermissionChecker.requireCreatorRole();
        return findActiveGroup(activityId) != null;
    }

    public boolean isMemberByActivity(Long activityId, Long userId) {
        ActivityGroup group = findActiveGroup(activityId);
        return group != null && isMember(group.getId(), userId);
    }

    public boolean canPrivateMessageInActivity(Long activityId, Long user1, Long user2) {
        ActivityGroup group = findActiveGroup(activityId);
        if (group == null) {
            return false;
        }
        return isMember(group.getId(), user1) && isMember(group.getId(), user2);
    }

    public Long getGroupIdForUser(Long activityId, Long userId) {
        ActivityGroup group = findActiveGroup(activityId);
        if (group == null || !isMember(group.getId(), userId)) {
            return null;
        }
        return group.getId();
    }

    @EventListener
    @Transactional
    public void onRegistrationApproved(RegistrationApprovedEvent event) {
        ActivityGroup group = findActiveGroup(event.activityId());
        if (group == null) {
            return;
        }
        if (addMemberInternal(group.getId(), event.userId(), 0)) {
            SysUser user = userService.getById(event.userId());
            String name = user != null ? user.getNickname() : "新成员";
            pushSystemMessage(group, name + " 加入了群聊");
        }
    }

    private void syncApprovedMembers(Long groupId, Long activityId) {
        List<ActivityRegistration> regs = registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, RegistrationStatus.APPROVED.getCode()));
        for (ActivityRegistration reg : regs) {
            addMemberInternal(groupId, reg.getUserId(), 0);
        }
    }

    private boolean addMemberInternal(Long groupId, Long userId, int role) {
        ActivityGroupMember existing = memberMapper.selectOne(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getGroupId, groupId)
                .eq(ActivityGroupMember::getUserId, userId));
        if (existing != null) {
            return false;
        }
        ActivityGroupMember member = new ActivityGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setLastReadTime(LocalDateTime.now());
        memberMapper.insert(member);
        return true;
    }

    private void pushSystemMessage(ActivityGroup group, String content) {
        ActivityGroupMessage msg = new ActivityGroupMessage();
        msg.setGroupId(group.getId());
        msg.setSenderId(0L);
        msg.setContent(content);
        msg.setMessageType(MSG_SYSTEM);
        msg.setStatus(1);
        messageMapper.insert(msg);
        group.setLastMessage(content.length() > 100 ? content.substring(0, 100) : content);
        group.setLastMessageTime(LocalDateTime.now());
        groupMapper.updateById(group);
    }

    private ActivityGroup findActiveGroup(Long activityId) {
        return groupMapper.selectOne(new LambdaQueryWrapper<ActivityGroup>()
                .eq(ActivityGroup::getActivityId, activityId)
                .eq(ActivityGroup::getStatus, 1));
    }

    private ActivityGroup requireGroup(Long groupId) {
        ActivityGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != 1) {
            throw new BusinessException("群聊不存在或已解散");
        }
        return group;
    }

    private void requireMember(Long groupId, Long userId) {
        if (!isMember(groupId, userId)) {
            throw new BusinessException(403, "您不在该群聊中");
        }
    }

    private boolean isMember(Long groupId, Long userId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getGroupId, groupId)
                .eq(ActivityGroupMember::getUserId, userId)) > 0;
    }

    private GroupVO toGroupVO(ActivityGroup group, Long userId) {
        GroupVO vo = new GroupVO();
        vo.setId(group.getId());
        vo.setActivityId(group.getActivityId());
        vo.setName(group.getName());
        vo.setCreatorId(group.getCreatorId());
        vo.setLastMessage(group.getLastMessage());
        vo.setLastMessageTime(group.getLastMessageTime());
        Activity activity = activityMapper.selectById(group.getActivityId());
        if (activity != null) {
            vo.setActivityTitle(activity.getTitle());
        }
        long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getGroupId, group.getId()));
        vo.setMemberCount((int) memberCount);
        vo.setUnreadCount(countUnreadForGroup(group.getId(), userId));
        return vo;
    }

    private long countUnreadForGroup(Long groupId, Long userId) {
        ActivityGroupMember member = memberMapper.selectOne(new LambdaQueryWrapper<ActivityGroupMember>()
                .eq(ActivityGroupMember::getGroupId, groupId)
                .eq(ActivityGroupMember::getUserId, userId));
        if (member == null) {
            return 0;
        }
        LambdaQueryWrapper<ActivityGroupMessage> wrapper = new LambdaQueryWrapper<ActivityGroupMessage>()
                .eq(ActivityGroupMessage::getGroupId, groupId)
                .eq(ActivityGroupMessage::getStatus, 1)
                .ne(ActivityGroupMessage::getSenderId, userId);
        if (member.getLastReadTime() != null) {
            wrapper.gt(ActivityGroupMessage::getCreatedAt, member.getLastReadTime());
        }
        return messageMapper.selectCount(wrapper);
    }

    @Data
    public static class GroupVO {
        private Long id;
        private Long activityId;
        private String activityTitle;
        private String name;
        private Long creatorId;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private Integer memberCount;
        private Long unreadCount;
    }

    @Data
    public static class MemberVO {
        private Long userId;
        private String nickname;
        private String avatar;
        private String college;
        private Integer role;
        private LocalDateTime joinedAt;
    }

    @Data
    public static class GroupMessageVO {
        private Long id;
        private Long groupId;
        private Long senderId;
        private String senderName;
        private String senderAvatar;
        private String content;
        private Integer messageType;
        private LocalDateTime createdAt;
    }
}
