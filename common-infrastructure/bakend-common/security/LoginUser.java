package com.campus.team.security;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LoginUser {
    private Long userId;
    private String openid;
    private Long adminId;
    private String username;
    private String roleCode;
    private String tokenType;
    private List<String> roles = new ArrayList<>();

    public boolean isAdmin() {
        return "admin".equals(tokenType);
    }

    public boolean isUser() {
        return "user".equals(tokenType);
    }
}
