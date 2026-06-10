package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.vo.ResultVO;
import com.opencode.agents.manager.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

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
