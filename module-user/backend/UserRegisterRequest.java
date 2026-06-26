package com.campus.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {
    @NotBlank(message = "请输入账号")
    @Size(min = 4, max = 32, message = "账号长度为4-32位")
    private String username;
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;
    @Size(max = 64, message = "昵称最多64字")
    private String nickname;
}
