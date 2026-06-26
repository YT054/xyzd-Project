package com.campus.team.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.team.data.entity.SysAdmin;
import com.campus.team.data.mapper.SysAdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminPasswordInitializer implements CommandLineRunner {

    private final SysAdminMapper adminMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        for (String username : new String[]{"admin", "ops"}) {
            SysAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                    .eq(SysAdmin::getUsername, username));
            if (admin != null) {
                admin.setPassword(encoder.encode("admin123"));
                adminMapper.updateById(admin);
            }
        }
    }
}
