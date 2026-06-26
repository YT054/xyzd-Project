package com.campus.team.common.enums;

import lombok.Getter;

@Getter
public enum RegistrationStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String label;

    RegistrationStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
