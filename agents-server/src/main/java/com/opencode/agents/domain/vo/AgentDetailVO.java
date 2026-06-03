package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentDetailVO {

    private Long id;
    private String name;
    private String description;
    private String avatar;
    private Integer currentVersion;
    private String systemPrompt;
    private String featuresJson;
    private LocalDateTime createTime;

}
