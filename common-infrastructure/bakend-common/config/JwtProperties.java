package com.campus.team.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "campus.jwt")
public class JwtProperties {
    private String secret;
    private int expireHours;
    private int adminExpireHours;
}
