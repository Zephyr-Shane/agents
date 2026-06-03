package com.opencode.agents.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.domain.dto.CreateConversationRequest;
import com.opencode.agents.domain.entity.Conversation;
import com.opencode.agents.domain.entity.Message;
import com.opencode.agents.domain.vo.ConversationVO;
import com.opencode.agents.domain.vo.MessageVO;
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

    @Override
    public List<ConversationVO> listConversations(Long userId, Long agentId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getDeleted, 0)
                .orderByDesc(Conversation::getUpdateTime);

        if (agentId == null) {
            wrapper.isNull(Conversation::getAgentId);
        } else {
            wrapper.eq(Conversation::getAgentId, agentId);
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
        conversation.setTitle(StrUtil.isNotBlank(request.getTitle())
                ? request.getTitle() : "新的对话");
        conversationMapper.insert(conversation);
        return toConversationVO(conversation);
    }

    @Override
    public List<MessageVO> getMessages(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreateTime)
        ).stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    private ConversationVO toConversationVO(Conversation c) {
        ConversationVO vo = new ConversationVO();
        vo.setId(c.getId());
        vo.setAgentId(c.getAgentId());
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
