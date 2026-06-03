package com.opencode.agents.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.common.JwtUtil;
import com.opencode.agents.domain.dto.LoginRequest;
import com.opencode.agents.domain.entity.User;
import com.opencode.agents.domain.vo.LoginResponse;
import com.opencode.agents.domain.vo.UserProfileVO;
import com.opencode.agents.mapper.UserMapper;
import com.opencode.agents.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 模拟微信登录：使用 code 作为 openid
        // 实际生产环境需调用微信接口: GET https://api.weixin.qq.com/sns/jscode2session
        String openid = request.getCode();
        if (openid == null || openid.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录code不能为空");
        }

        // 查找或创建用户
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, openid)
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("用户" + RandomUtil.randomString(6));
            user.setAvatar("");
            user.setRegisterTime(LocalDateTime.now());
            user.setLastLogin(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            user.setLastLogin(LocalDateTime.now());
            userMapper.updateById(user);
        }

        // 生成 token
        String token = jwtUtil.generateToken(user.getId());

        // 构造返回
        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setUid("UID_" + user.getId());
        profile.setRegisterTime(user.getRegisterTime());

        return new LoginResponse(token, profile);
    }

}
