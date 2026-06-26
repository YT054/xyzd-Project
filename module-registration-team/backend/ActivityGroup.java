package com.campus.team.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_group")
public class ActivityGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private String name;
    private Long creatorId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer status;
    private LocalDateTime createdAt;
}
