package com.opencode.agents.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.User;
import com.opencode.agents.domain.vo.UserProfileVO;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.UserMapper;
import com.opencode.agents.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AgentMapper agentMapper;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        long agentCount = agentMapper.selectCount(
                new LambdaQueryWrapper<Agent>()
                        .eq(Agent::getCreatorId, userId)
                        .eq(Agent::getDeleted, 0)
        );

        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setUid("UID_" + user.getId());
        vo.setRegisterTime(user.getRegisterTime());
        vo.setAgentCount((int) agentCount);
        return vo;
    }

}
