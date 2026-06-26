package com.campus.team.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "campus.admin")
public class AdminProperties {
    private List<String> allowedIps = new ArrayList<>();
}
