package com.opencode.agents.service;

import com.opencode.agents.domain.dto.CreateConversationRequest;
import com.opencode.agents.domain.vo.ConversationVO;
import com.opencode.agents.domain.vo.MessageVO;

import java.util.List;

public interface ConversationService {

    List<ConversationVO> listConversations(Long userId, Long agentId, String type);

    ConversationVO createConversation(Long userId, CreateConversationRequest request);

    List<MessageVO> getMessages(Long conversationId, Long userId);

}
