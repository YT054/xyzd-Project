package com.campus.team.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "campus.file")
public class FileStorageProperties {
    /** 本地存储根目录 */
    private String uploadDir = "./uploads";
    /** 对外访问基础 URL，如 http://localhost:8080/api */
    private String publicBaseUrl = "http://localhost:8080/api";
    /** 单文件最大 MB */
    private int maxSizeMb = 5;
    /** 允许的图片扩展名 */
    private List<String> allowedTypes = new ArrayList<>(List.of("jpg", "jpeg", "png", "gif", "webp"));
}
