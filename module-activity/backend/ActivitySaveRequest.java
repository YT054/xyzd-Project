package com.campus.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivitySaveRequest {
    private Long id;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "描述不能为空")
    private String description;
    private String coverImage;
    private String location;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime registerDeadline;
    @NotNull(message = "人数上限不能为空")
    private Integer maxMembers;
    private String tags;
}
