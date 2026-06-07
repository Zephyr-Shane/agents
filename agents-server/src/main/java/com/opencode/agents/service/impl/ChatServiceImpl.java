package com.opencode.agents.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.AgentVersion;
import com.opencode.agents.domain.entity.Conversation;
import com.opencode.agents.domain.entity.KnowledgeDoc;
import com.opencode.agents.domain.entity.Message;
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
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAgent> wrapper =
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserAgent>()
                                .eq(UserAgent::getUserId, userId)
                                .eq(UserAgent::getAgentId, agentId)
                                .set(UserAgent::getLastUsedTime, LocalDateTime.now());
                // 如果还没有关联记录，创建一个
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
                    userAgentMapper.update(null, wrapper);
                }
            } catch (Exception e) {
                log.warn("更新UserAgent最后使用时间失败", e);
            }
        }

        // 3. 保存用户消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setTokens(0);
        messageMapper.insert(userMsg);

        // 2.5 如果有上传文件，加载文件内容作为上下文
        String fileContext = "";
        if (fileIds != null && !fileIds.isEmpty()) {
            StringBuilder sb = new StringBuilder("\n\n【用户上传了以下文件】\n");
            for (Long fileId : fileIds) {
                KnowledgeDoc doc = knowledgeDocMapper.selectById(fileId);
                if (doc == null) continue;
                sb.append("\n--- 文件名: ").append(doc.getFileName()).append(" ---\n");
                // 尝试读取文本文件内容
                try {
                    java.nio.file.Path filePath = fileStorageService.getFilePath(doc.getFileUrl());
                    if (Files.exists(filePath)) {
                        String content = Files.readString(filePath);
                        // 限制文件内容长度，防止超出上下文窗口
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

        // 3. 构建 LLM messages
        List<ChatMessage> messages = new ArrayList<>();

        // 加载 system prompt
        String systemPrompt = getSystemPrompt(agentId);
        if (systemPrompt != null) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build());
        }

        // 如果有文件引用上下文，作为额外的系统消息注入
        if (!fileContext.isEmpty()) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(fileContext)
                    .build());
        }

        // 加载历史消息（最近20条）
        List<Message> history = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreateTime)
                        .last("limit 20")
        );

        for (Message msg : history) {
            messages.add(ChatMessage.builder()
                    .role("user".equals(msg.getRole()) ? ChatMessageRole.USER : ChatMessageRole.ASSISTANT)
                    .content(msg.getContent())
                    .build());
        }

        // 4. 发起 SSE 流式响应
        return sseManager.streamResponse(conversationId, aiManager, messages);
    }

    private String getSystemPrompt(Long agentId) {
        if (agentId == null) {
            return "你是一个智能AI助手，可以回答各种问题，帮助用户解决问题。请用中文回答。";
        }

        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) return null;

        AgentVersion version = agentVersionMapper.selectOne(
                new LambdaQueryWrapper<AgentVersion>()
                        .eq(AgentVersion::getAgentId, agentId)
                        .eq(AgentVersion::getVersion, agent.getCurrentVersion())
        );
        return version != null ? version.getSystemPrompt() : null;
    }

}
