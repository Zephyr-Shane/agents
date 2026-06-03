package com.opencode.agents.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    private Long conversationId;
    private Long agentId;
    private String message;
    private List<Long> fileIds;

}
