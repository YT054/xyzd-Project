package com.campus.team.infrastructure;

import com.campus.team.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUrlHelper {

    private final FileStorageProperties properties;

    /** 将数据库中的相对路径转为可访问的完整 URL */
    public String toAccessUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "";
        }
        if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
            return storedPath;
        }
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = storedPath.startsWith("/") ? storedPath : "/" + storedPath;
        return base + path;
    }

    /** 将前端传入的路径或完整 URL 规范化为数据库存储的相对路径 */
    public String toStoredPath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.startsWith("/files/")) {
            return value;
        }
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (value.startsWith(base + "/files/")) {
            return value.substring(base.length());
        }
        int idx = value.indexOf("/files/");
        if (idx >= 0) {
            return value.substring(idx);
        }
        return value;
    }
}
