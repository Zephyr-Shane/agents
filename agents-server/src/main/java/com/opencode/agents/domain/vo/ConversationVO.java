package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    private Long id;
    private Long agentId;
    /** 会话类型: general-普通对话, agent-智能体对话 */
    private String type;
    private String title;
    private String lastMessage;
    private Integer messageCount;
    private LocalDateTime updateTime;

}
