package com.campus.team.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserVO {
    private Long id;
    private String nickname;
    private String studentNo;
    private String college;
    private String phone;
    private Integer status;
    private Integer profileCompleted;
    private List<String> roles;
    private LocalDateTime createdAt;
}
