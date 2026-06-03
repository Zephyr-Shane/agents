package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.dto.LoginRequest;
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

    @PostMapping("/login")
    public ResultVO<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResultVO.success(authService.login(request));
    }

    @GetMapping("/profile")
    public ResultVO<UserProfileVO> profile() {
        Long userId = UserContext.getUserId();
        return ResultVO.success(userService.getProfile(userId));
    }

}
