package com.opencode.agents.domain.dto;

import lombok.Data;

@Data
public class CreateAgentRequest {

    /** 自然语言描述（用于 NL 创建方式） */
    private String description;

    // ========== 结构化创建字段（手动创建时使用） ==========

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
    /** 智能体类型 general-通用 super-超级，默认 general */
    private String type;

}
