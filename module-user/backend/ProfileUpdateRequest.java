package com.campus.team.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    private String avatar;
    private String phone;
    private String studentNo;
    private String college;
    @Min(0) @Max(2)
    private Integer gender;
    @Min(0) @Max(2)
    private Integer privacyLevel;
}
