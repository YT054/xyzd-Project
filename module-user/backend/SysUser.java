package com.campus.team.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String unionId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String studentNo;
    private String college;
    private Integer gender;
    private Integer privacyLevel;
    private Integer status;
    private Integer profileCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
