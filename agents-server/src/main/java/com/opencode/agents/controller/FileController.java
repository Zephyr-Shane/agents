package com.opencode.agents.controller;

import com.opencode.agents.manager.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 提供文件访问
     */
    @GetMapping("/{agentId}/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable Long agentId, @PathVariable String filename) {
        String relativePath = agentId + "/" + filename;
        try {
            Path filePath = fileStorageService.getFilePath(relativePath);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                        .body(resource);
            }
        } catch (Exception e) {
            log.warn("文件访问失败: {}", relativePath, e);
        }
        return ResponseEntity.notFound().build();
    }
}
