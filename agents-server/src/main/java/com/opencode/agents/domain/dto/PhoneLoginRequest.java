package com.opencode.agents.domain.dto;

import lombok.Data;

/**
 * 微信小程序手机号一键登录请求
 * 前端通过 wx.login + wx.getPhoneNumber 获取两个 code 后传给后端
 */
@Data
public class PhoneLoginRequest {

    /** wx.login() 获取的临时登录凭证 */
    private String loginCode;

    /** wx.getPhoneNumber() 返回的 code（新版，基础库 ≥ 2.21.2） */
    private String phoneCode;

    /** 用户昵称（可选，仅在首次登录时写入） */
    private String nickname;

    /** 用户头像 URL（可选） */
    private String avatarUrl;

}
