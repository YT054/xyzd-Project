package com.campus.team.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupCreateRequest {
    @NotNull(message = "活动不能为空")
    private Long activityId;
}
