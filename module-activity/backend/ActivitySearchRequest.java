package com.campus.team.api.dto;

import lombok.Data;

@Data
public class ActivitySearchRequest {
    private String keyword;
    private Long categoryId;
    private Integer activityStatus;
    private Long creatorId;
    private long page = 1;
    private long size = 10;
}
