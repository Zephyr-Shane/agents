package com.opencode.agents.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String uid;
    /** 手机号（手机号登录后才有值） */
    private String phone;
    private LocalDateTime registerTime;
    private Integer agentCount;

}
