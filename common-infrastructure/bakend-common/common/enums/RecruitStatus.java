package com.campus.team.common.enums;

import lombok.Getter;

@Getter
public enum RecruitStatus {
    OPEN(1, "招募中"),
    FULL(2, "已满员"),
    STOPPED(3, "已停止");

    private final int code;
    private final String label;

    RecruitStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
