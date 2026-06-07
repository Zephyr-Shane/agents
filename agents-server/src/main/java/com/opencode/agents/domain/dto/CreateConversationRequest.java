package com.opencode.agents.domain.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {

    private Long agentId;
    /** 会话类型: general-普通对话, agent-智能体对话 */
    private String type;
    private String title;

}
