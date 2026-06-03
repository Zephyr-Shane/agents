package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentVO {

    private Long id;
    private String name;
    private String description;
    private String avatar;
    private LocalDateTime createTime;

}
