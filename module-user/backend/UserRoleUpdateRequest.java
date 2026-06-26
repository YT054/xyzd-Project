package com.campus.team.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateRequest {
    @NotNull
    private List<String> roleCodes;
}
