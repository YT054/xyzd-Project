package com.campus.team.infrastructure;

import com.campus.team.common.exception.BusinessException;
import com.campus.team.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("avatar", "activity", "common");

    private final FileStorageProperties properties;
    private final FileUrlHelper fileUrlHelper;

    @PostConstruct
    public void init() throws IOException {
        Path root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        for (String type : ALLOWED_TYPES) {
            Files.createDirectories(root.resolve(type));
        }
        log.info("本地文件存储目录: {}", root);
    }

    public UploadResult upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (!ALLOWED_TYPES.contains(type)) {
            type = "common";
        }

        String ext = resolveExtension(file.getOriginalFilename(), file.getContentType());
        if (!properties.getAllowedTypes().contains(ext)) {
            throw new BusinessException("仅支持上传图片：" + String.join(", ", properties.getAllowedTypes()));
        }

        long maxBytes = properties.getMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片大小不能超过 " + properties.getMaxSizeMb() + "MB");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize()
                .resolve(type).resolve(filename);

        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }

        String storedPath = "/files/" + type + "/" + filename;
        UploadResult result = new UploadResult();
        result.setPath(storedPath);
        result.setUrl(fileUrlHelper.toAccessUrl(storedPath));
        return result;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (contentType != null) {
            return switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
        }
        return "jpg";
    }

    @lombok.Data
    public static class UploadResult {
        private String path;
        private String url;
    }
}
