package com.opencode.agents.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.common.JwtUtil;
import com.opencode.agents.domain.dto.LoginRequest;
import com.opencode.agents.domain.dto.PhoneLoginRequest;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.User;
import com.opencode.agents.domain.vo.LoginResponse;
import com.opencode.agents.domain.vo.UserProfileVO;
import com.opencode.agents.manager.WechatApiService;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.UserMapper;
import com.opencode.agents.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final JwtUtil jwtUtil;
    private final WechatApiService wechatApiService;

    // ========== 基础登录（静默登录） ==========

    @Override
    public LoginResponse login(LoginRequest request) {
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录code不能为空");
        }

        // jscode2session 换取 openid
        Map<String, String> session = wechatApiService.jscode2session(code);
        String openid = session.get("openid");

        // 查找或创建用户（通过 openid）
        User user = findOrCreateUserByOpenid(openid, session);

        return buildLoginResponse(user);
    }

    // ========== 手机号一键登录 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse phoneLogin(PhoneLoginRequest request) {
        if (request.getLoginCode() == null || request.getLoginCode().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录凭证不能为空");
        }
        if (request.getPhoneCode() == null || request.getPhoneCode().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号凭证不能为空");
        }

        // 1. jscode2session 换取 openid + session_key
        Map<String, String> session = wechatApiService.jscode2session(request.getLoginCode());
        String openid = session.get("openid");

        // 2. 获取微信绑定的手机号
        String phone = wechatApiService.getPhoneNumber(request.getPhoneCode());

        // 3. 查找用户：先用手机号查，查不到再用 openid 查
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .last("limit 1")
        );

        if (user == null) {
            // 手机号不存在，尝试用 openid 查找（可能是之前静默登录创建的）
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getOpenid, openid)
                            .last("limit 1")
            );
        }

        if (user == null) {
            // 全新用户 —— 创建完整账号
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(session.get("unionid"));
            user.setSessionKey(session.get("session_key"));
            user.setPhone(phone);
            user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                    ? request.getNickname() : "用户" + phone.substring(Math.max(0, phone.length() - 4)));
            user.setAvatar(request.getAvatarUrl() != null ? request.getAvatarUrl() : "");
            user.setDeleted(0);
            user.setRegisterTime(LocalDateTime.now());
            user.setLastLogin(LocalDateTime.now());
            userMapper.insert(user);
            log.info("手机号一键登录 - 新用户注册: phone={}, openid={}", maskPhone(phone), openid);
        } else {
            // 已有用户 —— 更新信息
            boolean needUpdate = false;
            if (!openid.equals(user.getOpenid())) {
                // 之前是用手机号查到的，但没有 openid（或不同 openid），补充绑定
                user.setOpenid(openid);
                needUpdate = true;
            }
            if (session.get("unionid") != null && !session.get("unionid").equals(user.getUnionid())) {
                user.setUnionid(session.get("unionid"));
                needUpdate = true;
            }
            if (!phone.equals(user.getPhone())) {
                user.setPhone(phone);
                needUpdate = true;
            }
            if (request.getNickname() != null && !request.getNickname().isBlank()
                    && !request.getNickname().equals(user.getNickname())) {
                user.setNickname(request.getNickname());
                needUpdate = true;
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                user.setAvatar(request.getAvatarUrl());
                needUpdate = true;
            }
            user.setSessionKey(session.get("session_key"));
            user.setLastLogin(LocalDateTime.now());
            if (needUpdate) {
                userMapper.updateById(user);
            } else {
                // 只更新 lastLogin
                userMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                                .eq(User::getId, user.getId())
                                .set(User::getLastLogin, LocalDateTime.now())
                                .set(User::getSessionKey, session.get("session_key"))
                );
            }
            log.info("手机号一键登录 - 已有用户登录: userId={}, phone={}", user.getId(), maskPhone(phone));
        }

        return buildLoginResponse(user);
    }

    // ========== 内部方法 ==========

    /**
     * 通过 openid 查找或创建用户（静默登录使用）
     */
    private User findOrCreateUserByOpenid(String openid, Map<String, String> session) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
                        .last("limit 1")
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(session.get("unionid"));
            user.setSessionKey(session.get("session_key"));
            user.setNickname("用户" + openid.substring(Math.max(0, openid.length() - 6)));
            user.setAvatar("");
            user.setDeleted(0);
            user.setRegisterTime(LocalDateTime.now());
            user.setLastLogin(LocalDateTime.now());
            userMapper.insert(user);
            log.info("静默登录 - 新用户: openid={}", openid);
        } else {
            user.setSessionKey(session.get("session_key"));
            if (session.get("unionid") != null) {
                user.setUnionid(session.get("unionid"));
            }
            user.setLastLogin(LocalDateTime.now());
            userMapper.updateById(user);
        }

        return user;
    }

    /**
     * 构建统一登录响应
     */
    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId());

        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setUid("UID_" + user.getId());
        profile.setPhone(user.getPhone());
        profile.setRegisterTime(user.getRegisterTime());
        // 智能体数量
        profile.setAgentCount(Math.toIntExact(agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>()
                        .eq(Agent::getCreatorId, user.getId())
                        .eq(Agent::getDeleted, 0)
        )));

        return new LoginResponse(token, profile);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
