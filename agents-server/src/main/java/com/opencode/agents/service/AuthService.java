package com.opencode.agents.service;

import com.opencode.agents.domain.dto.LoginRequest;
import com.opencode.agents.domain.vo.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

}
