package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.entity.KnowledgeDoc;
import com.opencode.agents.domain.vo.ResultVO;
import com.opencode.agents.manager.FileStorageService;
import com.opencode.agents.mapper.KnowledgeDocMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文件管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final FileStorageService fileStorageService;

    /**
     * 上传文件到指定智能体的知识库
     */
    @PostMapping("/upload/{agentId}")
    public ResultVO<KnowledgeDoc> uploadFile(
            @PathVariable Long agentId,
            @RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (file.isEmpty()) {
            return ResultVO.error(400, "请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return ResultVO.error(400, "文件名不能为空");
        }

        // 验证文件类型
        String fileType = getFileType(originalFilename);
        if (fileType == null) {
            return ResultVO.error(400, "不支持的文件类型，支持: txt, pdf, doc, docx, md");
        }

        // 存储文件
        try {
            String relativePath = fileStorageService.storeFile(file, agentId);

            // 记录到数据库
            KnowledgeDoc doc = new KnowledgeDoc();
            doc.setAgentId(agentId);
            doc.setFileName(originalFilename);
            doc.setFileUrl(relativePath);
            doc.setFileType(fileType);
            doc.setFileSize(file.getSize());
            knowledgeDocMapper.insert(doc);

            log.info("文件上传成功: agentId={}, fileName={}, size={}", agentId, originalFilename, file.getSize());
            return ResultVO.success(doc);
        } catch (Exception e) {
            log.error("文件上传失败: agentId={}, fileName={}", agentId, originalFilename, e);
            return ResultVO.error(1004, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定智能体的知识库文件列表
     */
    @GetMapping("/list/{agentId}")
    public ResultVO<List<KnowledgeDoc>> listFiles(@PathVariable Long agentId) {
        List<KnowledgeDoc> docs = knowledgeDocMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDoc>()
                        .eq(KnowledgeDoc::getAgentId, agentId)
                        .orderByDesc(KnowledgeDoc::getCreateTime)
        );
        return ResultVO.success(docs);
    }

    /**
     * 删除知识库文件
     */
    @DeleteMapping("/{docId}")
    public ResultVO<Void> deleteFile(@PathVariable Long docId) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            return ResultVO.error(404, "文件不存在");
        }

        // 删除物理文件
        fileStorageService.deleteFile(doc.getFileUrl());
        // 删除数据库记录
        knowledgeDocMapper.deleteById(docId);

        log.info("文件删除成功: docId={}, fileName={}", docId, doc.getFileName());
        return ResultVO.success();
    }

    /**
     * 根据文件名获取文件类型
     */
    private String getFileType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".txt")) return "txt";
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".doc")) return "doc";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".md")) return "md";
        return null;
    }
}
