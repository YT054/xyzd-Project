package com.campus.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplaintRequest {
    @NotNull
    private Integer targetType;
    @NotNull
    private Long targetId;
    @NotBlank
    private String reason;
}
