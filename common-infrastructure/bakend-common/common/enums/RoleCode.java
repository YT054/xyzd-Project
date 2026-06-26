package com.campus.team.common.enums;

import lombok.Getter;

@Getter
public enum RoleCode {
    USER("USER"),
    CREATOR("CREATOR"),
    CAMPUS_ADMIN("CAMPUS_ADMIN"),
    OPS_ADMIN("OPS_ADMIN");

    private final String code;

    RoleCode(String code) {
        this.code = code;
    }
}
