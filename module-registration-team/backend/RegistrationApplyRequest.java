package com.campus.team.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrationApplyRequest {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    private String applyMessage;
}
