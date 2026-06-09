package com.opencode.agents.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.common.JwtUtil;
import com.opencode.agents.domain.dto.*;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.User;
import com.opencode.agents.domain.vo.LoginResponse;
import com.opencode.agents.domain.vo.UserProfileVO;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.UserMapper;
import com.opencode.agents.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AgentMapper agentMapper;
    private final JwtUtil jwtUtil;

    // ========== 密码登录 ==========

    @Override
    public LoginResponse passwordLogin(PasswordLoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }

        String username = request.getUsername().trim();
        String passwordHash = DigestUtil.sha256Hex(request.getPassword());

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("limit 1")
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在，请先注册");
        }
        if (!passwordHash.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "密码错误");
        }

        user.setLastLogin(LocalDateTime.now());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLastLogin, LocalDateTime.now()));

        return buildLoginResponse(user);
    }

    // ========== 注册 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
        }

        String username = request.getUsername().trim();
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("limit 1")
        );
        if (exist != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtil.sha256Hex(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setAvatar("");
        user.setDeleted(0);
        user.setRegisterTime(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        userMapper.insert(user);

        log.info("新用户注册: username={}, nickname={}", username, request.getNickname());
        return buildLoginResponse(user);
    }

    // ========== 更新个人资料 ==========

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        boolean needUpdate = false;
        if (request.getNickname() != null && !request.getNickname().isBlank()
                && !request.getNickname().equals(user.getNickname())) {
            user.setNickname(request.getNickname());
            needUpdate = true;
        }
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user.setAvatar(request.getAvatar());
            needUpdate = true;
        }
        if (needUpdate) {
            userMapper.updateById(user);
            log.info("更新用户资料: userId={}, hasNickname={}, hasAvatar={}",
                    userId,
                    request.getNickname() != null && !request.getNickname().isBlank(),
                    request.getAvatar() != null && !request.getAvatar().isBlank());
        }
    }

    // ========== 内部方法 ==========

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId());
        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setUid("UID_" + user.getId());
        profile.setPhone(user.getPhone());
        profile.setRegisterTime(user.getRegisterTime());
        profile.setAgentCount(Math.toIntExact(agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>().eq(Agent::getCreatorId, user.getId()).eq(Agent::getDeleted, 0))));
        return new LoginResponse(token, profile);
    }
}
