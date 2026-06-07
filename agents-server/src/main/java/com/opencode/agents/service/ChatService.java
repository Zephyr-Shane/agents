package com.opencode.agents.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    SseEmitter streamChat(Long userId, Long conversationId, Long agentId,
                          String message, List<Long> fileIds);

}
