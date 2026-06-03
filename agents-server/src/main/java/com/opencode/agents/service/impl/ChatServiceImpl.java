package com.opencode.agents.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.AgentVersion;
import com.opencode.agents.domain.entity.Conversation;
import com.opencode.agents.domain.entity.Message;
import com.opencode.agents.manager.AiManager;
import com.opencode.agents.manager.SseManager;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.AgentVersionMapper;
import com.opencode.agents.mapper.ConversationMapper;
import com.opencode.agents.mapper.MessageMapper;
import com.opencode.agents.service.ChatService;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final AiManager aiManager;
    private final SseManager sseManager;

    @Override
    public SseEmitter streamChat(Long userId, Long conversationId, Long agentId, String message) {
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

        // 2. 保存用户消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        userMsg.setTokens(0);
        messageMapper.insert(userMsg);

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
