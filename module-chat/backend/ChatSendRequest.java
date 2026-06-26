package com.campus.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatSendRequest {
    @NotNull
    private Long activityId;
    @NotNull
    private Long receiverId;
    @NotBlank
    private String content;
}
