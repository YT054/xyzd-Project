package com.campus.team.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfileVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private String studentNo;
    private String college;
    private Integer gender;
    private Integer privacyLevel;
    private Integer profileCompleted;
    private List<String> roles;
    private long unreadMessages;
}
