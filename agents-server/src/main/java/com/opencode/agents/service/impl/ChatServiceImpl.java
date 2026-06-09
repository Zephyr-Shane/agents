package com.opencode.agents.service.impl;

import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.AgentVersion;
import com.opencode.agents.domain.entity.Conversation;
import com.opencode.agents.domain.entity.KnowledgeDoc;
import com.opencode.agents.domain.entity.UserAgent;
import com.opencode.agents.manager.AiManager;
import com.opencode.agents.manager.FileStorageService;
import com.opencode.agents.manager.SseManager;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.AgentVersionMapper;
import com.opencode.agents.mapper.ConversationMapper;
import com.opencode.agents.mapper.KnowledgeDocMapper;
import com.opencode.agents.mapper.MessageMapper;
import com.opencode.agents.mapper.UserAgentMapper;
import com.opencode.agents.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final AgentMapper agentMapper;
    private final AgentVersionMapper agentVersionMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final UserAgentMapper userAgentMapper;
    private final FileStorageService fileStorageService;
    private final AiManager aiManager;
    private final SseManager sseManager;

    @Override
    public SseEmitter streamChat(Long userId, Long conversationId, Long agentId,
                                 String message, List<Long> fileIds) {
        // 1. 查找或创建会话
        Conversation conversation;
        if (conversationId == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setAgentId(agentId);
            conversation.setType(agentId != null ? "agent" : "general");
            conversation.setTitle(message.length() > 30 ? message.substring(0, 30) + "..." : message);
            conversationMapper.insert(conversation);
            conversationId = conversation.getId();
        } else {
            conversation = conversationMapper.selectById(conversationId);
            if (conversation == null || !conversation.getUserId().equals(userId)) {
                throw new RuntimeException("会话不存在");
            }
        }

        // 2. 更新用户-智能体关联的最后使用时间
        if (agentId != null) {
            try {
                UserAgent ua = userAgentMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAgent>()
                                .eq(UserAgent::getUserId, userId)
                                .eq(UserAgent::getAgentId, agentId)
                );
                if (ua == null) {
                    ua = new UserAgent();
                    ua.setUserId(userId);
                    ua.setAgentId(agentId);
                    ua.setIsCreator(0);
                    ua.setIsFavorite(0);
                    ua.setFirstUsedTime(LocalDateTime.now());
                    ua.setLastUsedTime(LocalDateTime.now());
                    userAgentMapper.insert(ua);
                } else {
                    userAgentMapper.update(null,
                            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAgent>()
                                    .eq(UserAgent::getUserId, userId)
                                    .eq(UserAgent::getAgentId, agentId)
                                    .set(UserAgent::getLastUsedTime, LocalDateTime.now()));
                }
            } catch (Exception e) {
                log.warn("更新UserAgent最后使用时间失败", e);
            }
        }

        // 3. 保存用户消息并预填对话记忆（服务重启后从 DB 恢复上下文）
        com.opencode.agents.domain.entity.Message userMsgEntity = new com.opencode.agents.domain.entity.Message();
        userMsgEntity.setConversationId(conversationId);
        userMsgEntity.setRole("user");
        userMsgEntity.setContent(message);
        userMsgEntity.setTokens(0);
        messageMapper.insert(userMsgEntity);

        String convIdStr = String.valueOf(conversationId);
        populateMemoryIfNeeded(convIdStr);

        // 4. 如果有上传文件，加载文件内容作为上下文
        String fileContext = "";
        if (fileIds != null && !fileIds.isEmpty()) {
            StringBuilder sb = new StringBuilder("\n\n【用户上传了以下文件】\n");
            for (Long fileId : fileIds) {
                KnowledgeDoc doc = knowledgeDocMapper.selectById(fileId);
                if (doc == null) continue;
                sb.append("\n--- 文件名: ").append(doc.getFileName()).append(" ---\n");
                try {
                    java.nio.file.Path filePath = fileStorageService.getFilePath(doc.getFileUrl());
                    if (Files.exists(filePath)) {
                        String content = Files.readString(filePath);
                        if (content.length() > 5000) {
                            content = content.substring(0, 5000) + "\n...（文件过长，已截断至前5000字）";
                        }
                        sb.append(content);
                    } else {
                        sb.append("（文件不可读，文件名仅供参考）\n");
                    }
                } catch (Exception e) {
                    log.warn("读取文件内容失败: docId={}", fileId, e);
                    sb.append("（文件读取失败: ").append(e.getMessage()).append("）\n");
                }
                sb.append("\n--- 文件结束 ---\n");
            }
            sb.append("\n请基于以上文件内容回答用户的问题。");
            fileContext = sb.toString();
            log.info("已加载 {} 个文件作为对话上下文", fileIds.size());
        }

        // 5. 构建 system prompt（含文件上下文）
        String systemPrompt = getSystemPrompt(agentId);
        if (!fileContext.isEmpty()) {
            systemPrompt = (systemPrompt != null ? systemPrompt + "\n\n" : "") + fileContext;
        }

        // 6. 发起 SSE 流式响应（Spring AI 的 MessageChatMemoryAdvisor 自动管理多轮记忆）
        boolean needsTitle = conversationId == null
                || "新的对话".equals(conversation.getTitle())
                || "智能体对话".equals(conversation.getTitle());
        String userMessageText = message;

        return sseManager.streamResponse(conversationId,
                aiManager.streamChat(systemPrompt, message, convIdStr),
                needsTitle ? (assistantReply) ->
                        generateTitle(conversation, userMessageText, assistantReply) : null);
    }

    /**
     * 若对话记忆为空，从数据库加载历史并预填
     */
    private void populateMemoryIfNeeded(String conversationId) {
        List<com.opencode.agents.domain.entity.Message> dbMessages = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.opencode.agents.domain.entity.Message>()
                        .eq(com.opencode.agents.domain.entity.Message::getConversationId, conversationId)
                        .orderByAsc(com.opencode.agents.domain.entity.Message::getCreateTime)
        );
        if (dbMessages.size() <= 1) return; // 只有当前这条用户消息，无需恢复

        List<org.springframework.ai.chat.messages.Message> history = new ArrayList<>();
        for (var dbMsg : dbMessages) {
            if ("system".equals(dbMsg.getRole())) {
                history.add(new SystemMessage(dbMsg.getContent()));
            } else if ("user".equals(dbMsg.getRole())) {
                history.add(new UserMessage(dbMsg.getContent()));
            } else if ("assistant".equals(dbMsg.getRole())) {
                history.add(new AssistantMessage(dbMsg.getContent()));
            }
        }
        aiManager.populateMemory(conversationId, history);
    }

    /**
     * 异步生成对话标题并更新数据库
     */
    private void generateTitle(Conversation conversation, String userMsg, String assistantReply) {
        try {
            List<org.springframework.ai.chat.messages.Message> titleMessages = new ArrayList<>();
            titleMessages.add(new SystemMessage(
                    "根据以下对话内容生成一个简洁的标题（3-8个字），直接输出标题，不要多余内容。\n\n" +
                    "用户: " + userMsg + "\n助手: " + (assistantReply.length() > 100 ?
                            assistantReply.substring(0, 100) : assistantReply)));
            ChatResponse response = aiManager.syncChat(titleMessages);
            if (response != null && response.getResult() != null
                    && response.getResult().getOutput() != null) {
                String title = response.getResult().getOutput().getText().trim();
                title = title.replaceAll("^[\"\'「」]+|[\"\'「」]+$", "");
                if (title.length() > 50) title = title.substring(0, 50);
                if (!title.isEmpty()) {
                    conversation.setTitle(title);
                    conversationMapper.updateById(conversation);
                    log.info("对话标题已自动生成: id={}, title={}", conversation.getId(), title);
                }
            }
        } catch (Exception e) {
            log.warn("自动生成对话标题失败: id={}", conversation.getId(), e);
        }
    }

    private String getSystemPrompt(Long agentId) {
        if (agentId == null) {
            return "你是一个智能AI助手，可以回答各种问题，帮助用户解决问题。请用中文回答。";
        }
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) return null;
        AgentVersion version = agentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentVersion>()
                        .eq(AgentVersion::getAgentId, agentId)
                        .eq(AgentVersion::getVersion, agent.getCurrentVersion())
        );
        return version != null ? version.getSystemPrompt() : null;
    }

}
