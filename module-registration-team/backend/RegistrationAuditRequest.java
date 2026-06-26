package com.campus.team.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrationAuditRequest {
    @NotNull
    private Long registrationId;
    @NotNull
    private Boolean approved;
    private String auditMessage;
}
