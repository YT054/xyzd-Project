package com.campus.team.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_group_message")
public class ActivityGroupMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long senderId;
    private String content;
    private Integer messageType;
    private Integer status;
    private LocalDateTime createdAt;
}
