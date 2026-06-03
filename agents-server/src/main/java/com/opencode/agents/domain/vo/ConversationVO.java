package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    private Long id;
    private Long agentId;
    private String title;
    private String lastMessage;
    private Integer messageCount;
    private LocalDateTime updateTime;

}
