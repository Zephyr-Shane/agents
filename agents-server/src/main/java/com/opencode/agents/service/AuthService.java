package com.opencode.agents.service;

import com.opencode.agents.domain.dto.LoginRequest;
import com.opencode.agents.domain.dto.PhoneLoginRequest;
import com.opencode.agents.domain.vo.LoginResponse;

public interface AuthService {

    /**
     * 基础登录（wx.login → openid）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 微信小程序手机号一键登录
     * 通过 wx.login + wx.getPhoneNumber 获取用户手机号并完成登录/注册
     */
    LoginResponse phoneLogin(PhoneLoginRequest request);

}
