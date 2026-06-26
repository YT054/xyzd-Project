package com.campus.team.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.team.data.entity.ActivityGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityGroupMemberMapper extends BaseMapper<ActivityGroupMember> {

    @Select("""
            SELECT COUNT(*) FROM activity_group_message m
            INNER JOIN activity_group_member gm ON gm.group_id = m.group_id AND gm.user_id = #{userId}
            WHERE m.status = 1 AND m.sender_id != #{userId}
              AND (gm.last_read_time IS NULL OR m.created_at > gm.last_read_time)
            """)
    long countUnread(@Param("userId") Long userId);
}
