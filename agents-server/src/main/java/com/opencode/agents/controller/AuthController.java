package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.dto.LoginRequest;
import com.opencode.agents.domain.dto.PhoneLoginRequest;
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

    /**
     * 微信小程序手机号一键登录
     * 前端需要先调用 wx.login() 获取 loginCode，
     * 再通过 button[open-type="getPhoneNumber"] 获取 phoneCode，
     * 后端通过这两个 code 换取用户手机号并完成登录/注册
     */
    @PostMapping("/phone-login")
    public ResultVO<LoginResponse> phoneLogin(@RequestBody PhoneLoginRequest request) {
        return ResultVO.success(authService.phoneLogin(request));
    }

    @GetMapping("/profile")
    public ResultVO<UserProfileVO> profile() {
        Long userId = UserContext.getUserId();
        return ResultVO.success(userService.getProfile(userId));
    }

}
