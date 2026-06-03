package com.opencode.agents.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opencode.agents.domain.entity.UserAgent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAgentMapper extends BaseMapper<UserAgent> {
}
