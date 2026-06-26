package com.campus.team.common.enums;

import lombok.Getter;

@Getter
public enum ActivityStatus {
    RECRUITING(1, "招募中"),
    ONGOING(2, "进行中"),
    FINISHED(3, "已结束"),
    OFFLINE(4, "已下架");

    private final int code;
    private final String label;

    ActivityStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isVisible(int code) {
        return code != OFFLINE.code;
    }
}
