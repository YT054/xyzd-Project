package com.campus.team.api;

import com.campus.team.api.dto.*;
import com.campus.team.api.vo.LoginVO;
import com.campus.team.api.vo.UserProfileVO;
import com.campus.team.common.Result;
import com.campus.team.core.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.ok(userService.accountLogin(request));
    }

    @PostMapping("/wx-login")
    public Result<LoginVO> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.ok(userService.wxLogin(request));
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.ok(userService.getProfile());
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(request));
    }
}
