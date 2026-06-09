package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.vo.ResultVO;
import com.opencode.agents.manager.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 提供文件访问,支持多级路径
     */
    @GetMapping("/api/files/{*path}")
    public ResponseEntity<Resource> getFile(@PathVariable String path) {
        try {
            Path filePath = fileStorageService.getFilePath(path);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String encodedFilename = URLEncoder.encode(path, StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                        .body(resource);
            }
        } catch (Exception e) {
            log.warn("文件访问失败: {}", path, e);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 上传头像(需要登录)
     */
    @PostMapping("/api/upload/avatar")
    public ResultVO<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResultVO.error(401, "未登录");
        }
        if (file.isEmpty()) {
            return ResultVO.error(400, "请选择要上传的文件");
        }
        try {
            String relativePath = fileStorageService.storeAvatar(userId, file);
            String url = buildFullUrl(request, fileStorageService.getFileUrl(relativePath));
            return ResultVO.success(url);
        } catch (Exception e) {
            log.error("头像上传失败: userId={}", userId, e);
            return ResultVO.error(1004, "头像上传失败: " + e.getMessage());
        }
    }

    private String buildFullUrl(HttpServletRequest request, String path) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        return scheme + "://" + host + ":" + port + path;
    }
}
