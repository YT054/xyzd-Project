package com.campus.team.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.team.data.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT COUNT(*) FROM chat_message WHERE receiver_id = #{userId} AND is_read = 0 AND status = 1")
    long countUnread(Long userId);
}
