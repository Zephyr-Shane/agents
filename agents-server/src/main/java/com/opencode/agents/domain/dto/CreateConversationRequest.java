package com.opencode.agents.domain.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {

    private Long agentId;
    private String title;

}
