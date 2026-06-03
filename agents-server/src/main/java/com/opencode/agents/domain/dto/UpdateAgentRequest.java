package com.opencode.agents.domain.dto;

import lombok.Data;

@Data
public class UpdateAgentRequest {

    private Long agentId;
    private String description;

}
