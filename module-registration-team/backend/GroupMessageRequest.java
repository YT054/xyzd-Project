package com.campus.team.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupMessageRequest {
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
