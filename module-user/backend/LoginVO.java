package com.campus.team.api.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private boolean profileCompleted;
    private List<String> roles;
}
