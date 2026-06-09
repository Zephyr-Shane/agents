package com.opencode.agents.service;

import com.opencode.agents.domain.dto.*;
import com.opencode.agents.domain.vo.LoginResponse;

public interface AuthService {

    LoginResponse passwordLogin(PasswordLoginRequest request);

    LoginResponse register(RegisterRequest request);

    void updateProfile(Long userId, UpdateProfileRequest request);

}
