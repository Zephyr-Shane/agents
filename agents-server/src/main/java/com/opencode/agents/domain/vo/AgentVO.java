package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentVO {

    private Long id;
    private String name;
    private String description;
    private String introduction;
    private String openingLine;
    private String avatar;
    private String type;
    private Integer isPublic;
    private LocalDateTime createTime;

}
