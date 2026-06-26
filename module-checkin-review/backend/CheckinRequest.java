package com.campus.team.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckinRequest {
    @NotNull
    private Long activityId;
    private String content;
}
