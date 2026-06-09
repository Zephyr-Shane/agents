package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.dto.*;
import com.opencode.agents.domain.vo.LoginResponse;
import com.opencode.agents.domain.vo.ResultVO;
import com.opencode.agents.domain.vo.UserProfileVO;
import com.opencode.agents.service.AuthService;
import com.opencode.agents.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/password-login")
    public ResultVO<LoginResponse> passwordLogin(@RequestBody PasswordLoginRequest request) {
        return ResultVO.success(authService.passwordLogin(request));
    }

    @PostMapping("/register")
    public ResultVO<LoginResponse> register(@RequestBody RegisterRequest request) {
        return ResultVO.success(authService.register(request));
    }

    @PostMapping("/update-profile")
    public ResultVO<Void> updateProfile(@RequestBody UpdateProfileRequest request) {
        authService.updateProfile(UserContext.getUserId(), request);
        return ResultVO.success();
    }

    @GetMapping("/profile")
    public ResultVO<UserProfileVO> profile() {
        Long userId = UserContext.getUserId();
        return ResultVO.success(userService.getProfile(userId));
    }

}
