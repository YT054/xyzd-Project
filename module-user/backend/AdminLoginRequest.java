package com.campus.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminLoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
