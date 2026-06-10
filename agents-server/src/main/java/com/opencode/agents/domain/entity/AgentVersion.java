package com.opencode.agents.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_version")
public class AgentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;
    private Integer version;
    private String systemPrompt;
    private String featuresJson;
    private String description;
    private String changeLog;
    private Long createdBy;
    /** 回滚来源版本号（0=正常创建，>0=从该版本回滚而来） */
    private Integer rollbackSourceVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
