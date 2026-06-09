package com.opencode.agents.manager;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件存储服务
 * 使用本地文件系统存储，后续可扩展为阿里云OSS
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("文件存储目录初始化完成: {}", uploadDir);
        } catch (IOException e) {
            log.error("初始化文件存储目录失败: {}", uploadDir, e);
        }
    }

    /**
     * 存储头像文件
     */
    public String storeAvatar(Long userId, MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + extension;
        String relativePath = "avatars/" + userId + "/" + filename;
        try {
            Path targetPath = Paths.get(uploadDir, relativePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("头像存储成功: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            log.error("头像存储失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    /**
     * 存储文件，返回存储后的文件路径（相对路径）
     */
    public String storeFile(MultipartFile file, Long agentId) {
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + extension;
        String relativePath = agentId + "/" + filename;

        try {
            Path targetPath = Paths.get(uploadDir, relativePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件存储成功: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            log.error("文件存储失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的绝对路径
     */
    public Path getFilePath(String relativePath) {
        return Paths.get(uploadDir, relativePath);
    }

    /**
     * 获取文件的访问URL（本地模式返回相对路径，生产环境返回完整URL）
     */
    public String getFileUrl(String relativePath) {
        return "/api/files/" + relativePath;
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String relativePath) {
        try {
            return Files.deleteIfExists(Paths.get(uploadDir, relativePath));
        } catch (IOException e) {
            log.warn("文件删除失败: {}", relativePath, e);
            return false;
        }
    }
}
