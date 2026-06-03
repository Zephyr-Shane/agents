package com.opencode.agents.service;

import com.opencode.agents.domain.vo.ConversationVO;
import com.opencode.agents.domain.vo.MessageVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    SseEmitter streamChat(Long userId, Long conversationId, Long agentId, String message);

}
