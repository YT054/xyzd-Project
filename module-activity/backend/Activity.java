package com.campus.team.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long creatorId;
    private Long categoryId;
    private String title;
    private String description;
    private String coverImage;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registerDeadline;
    private Integer maxMembers;
    private Integer currentMembers;
    private Integer recruitStatus;
    private Integer activityStatus;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
