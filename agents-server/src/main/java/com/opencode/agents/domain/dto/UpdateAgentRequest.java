package com.opencode.agents.domain.dto;

import lombok.Data;

@Data
public class UpdateAgentRequest {

    /** 智能体 ID（用于 NL 更新方式） */
    private Long agentId;

    /** 自然语言描述（用于 NL 更新方式） */
    private String description;

    // ========== 结构化更新字段 ==========

    /** 智能体名称 */
    private String name;
    /** 功能描述 */
    private String agentDescription;
    /** 介绍 */
    private String introduction;
    /** 开场白 */
    private String openingLine;
    /** 头像 URL */
    private String avatar;
    /** 是否公开（1=公开，0=私有） */
    private Integer isPublic;

}
