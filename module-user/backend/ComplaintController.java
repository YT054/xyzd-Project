package com.campus.team.api;

import com.campus.team.api.dto.ComplaintRequest;
import com.campus.team.common.Result;
import com.campus.team.core.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final AdminService adminService;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody ComplaintRequest request) {
        adminService.submitComplaint(request);
        return Result.ok();
    }
}
