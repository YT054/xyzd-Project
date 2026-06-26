package com.campus.team.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "campus.wechat")
public class WeChatProperties {
    private String appId;
    private String appSecret;
    private boolean mockEnabled;
}
