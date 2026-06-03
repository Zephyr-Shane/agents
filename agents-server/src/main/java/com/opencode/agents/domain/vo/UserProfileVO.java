package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String uid;
    private LocalDateTime registerTime;
    private Integer agentCount;

}
