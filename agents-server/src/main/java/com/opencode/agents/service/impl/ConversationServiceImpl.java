package com.opencode.agents.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.domain.dto.CreateConversationRequest;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.Conversation;
import com.opencode.agents.domain.entity.Message;
import com.opencode.agents.domain.vo.ConversationVO;
import com.opencode.agents.domain.vo.MessageVO;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.ConversationMapper;
import com.opencode.agents.mapper.MessageMapper;
import com.opencode.agents.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final AgentMapper agentMapper;

    @Override
    public List<ConversationVO> listConversations(Long userId, Long agentId, String type) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getDeleted, 0)
                .orderByDesc(Conversation::getUpdateTime);

        if (agentId != null) {
            // 查询指定智能体的对话
            wrapper.eq(Conversation::getAgentId, agentId);
            // 如果同时传了 type，追加过滤
            if (type != null) {
                wrapper.eq(Conversation::getType, type);
            }
        } else if ("general".equals(type)) {
            // 显式查询普通对话
            wrapper.isNull(Conversation::getAgentId)
                   .eq(Conversation::getType, "general");
        } else {
            // 未指定任何条件：返回所有对话（兼容旧调用）
        }

        return conversationMapper.selectList(wrapper).stream()
                .map(this::toConversationVO)
                .collect(Collectors.toList());
    }

    @Override
    public ConversationVO createConversation(Long userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setAgentId(request.getAgentId());

        // 根据 agentId 自动推断 type
        String type = request.getType();
        if (type == null) {
            type = request.getAgentId() != null ? "agent" : "general";
        }
        conversation.setType(type);

        String title = request.getTitle();
        if (StrUtil.isBlank(title)) {
            if ("agent".equals(type) && request.getAgentId() != null) {
                Agent agent = agentMapper.selectById(request.getAgentId());
                title = (agent != null && StrUtil.isNotBlank(agent.getName())) ? agent.getName() : "智能体对话";
            } else {
                title = "新的对话";
            }
        }
        conversation.setTitle(title);
        conversationMapper.insert(conversation);
        return toConversationVO(conversation);
    }

    @Override
    public List<MessageVO> getMessages(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreateTime)
        );

        // 智能体对话没有任何消息时，注入开场白
        if (messages.isEmpty() && conversation.getAgentId() != null) {
            Agent agent = agentMapper.selectById(conversation.getAgentId());
            if (agent != null && StrUtil.isNotBlank(agent.getOpeningLine())) {
                Message opening = new Message();
                opening.setConversationId(conversationId);
                opening.setRole("assistant");
                opening.setContent(agent.getOpeningLine());
                opening.setTokens(0);
                messageMapper.insert(opening);
                messages.add(opening);
            }
        }

        return messages.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    private ConversationVO toConversationVO(Conversation c) {
        ConversationVO vo = new ConversationVO();
        vo.setId(c.getId());
        vo.setAgentId(c.getAgentId());
        vo.setType(c.getType());
        vo.setTitle(c.getTitle());
        vo.setUpdateTime(c.getUpdateTime());

        LambdaQueryWrapper<Message> countWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, c.getId());
        vo.setMessageCount(messageMapper.selectCount(countWrapper).intValue());

        LambdaQueryWrapper<Message> lastMsgWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, c.getId())
                .orderByDesc(Message::getCreateTime)
                .last("limit 1");
        Message lastMsg = messageMapper.selectOne(lastMsgWrapper);
        if (lastMsg != null) {
            String content = lastMsg.getContent();
            vo.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        }

        return vo;
    }

    private MessageVO toMessageVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }

}
