package com.campus.team.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityVO {
    private Long id;
    private Long creatorId;
    private String creatorName;
    private Long categoryId;
    private String categoryName;
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
    private Integer myRegistrationStatus;
    private Boolean isCreator;
    private Boolean myHasCheckin;
    private Boolean myHasReview;
    private Long groupId;
    private Boolean inGroup;
    private Boolean registerClosed;
}
