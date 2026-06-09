package com.opencode.agents.domain.dto;

import lombok.Data;

@Data
public class PasswordLoginRequest {

    private String username;
    private String password;

}
