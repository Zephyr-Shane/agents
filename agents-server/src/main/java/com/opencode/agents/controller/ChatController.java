package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.dto.ChatRequest;
import com.opencode.agents.domain.dto.CreateConversationRequest;
import com.opencode.agents.domain.vo.ConversationVO;
import com.opencode.agents.domain.vo.MessageVO;
import com.opencode.agents.domain.vo.ResultVO;
import com.opencode.agents.service.ChatService;
import com.opencode.agents.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResultVO<List<ConversationVO>> listConversations(
            @RequestParam(required = false) Long agentId) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(conversationService.listConversations(userId, agentId));
    }

    @PostMapping("/conversations")
    public ResultVO<ConversationVO> createConversation(@RequestBody CreateConversationRequest request) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(conversationService.createConversation(userId, request));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResultVO<List<MessageVO>> getMessages(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(conversationService.getMessages(id, userId));
    }

    @PostMapping("/chat/stream")
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        Long userId = UserContext.getUserId();
        return chatService.streamChat(userId, request.getConversationId(),
                request.getAgentId(), request.getMessage(), request.getFileIds());
    }

}
