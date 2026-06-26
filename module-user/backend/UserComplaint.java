package com.campus.team.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_complaint")
public class UserComplaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    private Integer targetType;
    private Long targetId;
    private String reason;
    private Integer status;
    private Long handlerId;
    private String handleRemark;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
