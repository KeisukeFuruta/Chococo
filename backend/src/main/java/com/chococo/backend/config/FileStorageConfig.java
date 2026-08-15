package com.chococo.backend.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// aws-infra-design.md 3.4節：写真はUUIDファイル名でディスク保存する（本番はEBS、Nginxが/uploads/*を直接配信）。
// /uploads/**へのリソースハンドラはローカル開発での動作確認用（本番ではNginxが先に処理するため実質未使用）。
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public FileStorageConfig(@Value("${chococo.storage.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @PostConstruct
    void ensureUploadDirExists() throws IOException {
        Files.createDirectories(Path.of(uploadDir));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Path.of(uploadDir).toAbsolutePath() + "/");
    }
}
