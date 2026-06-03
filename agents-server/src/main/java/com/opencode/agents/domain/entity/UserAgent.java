package com.opencode.agents.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_agent")
public class UserAgent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long agentId;
    private Integer isCreator;
    private Integer isFavorite;
    private LocalDateTime firstUsedTime;
    private LocalDateTime lastUsedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
