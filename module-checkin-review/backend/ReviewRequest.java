package com.campus.team.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private Long activityId;
    @NotNull @Min(1) @Max(5)
    private Integer rating;
    private String content;
}
