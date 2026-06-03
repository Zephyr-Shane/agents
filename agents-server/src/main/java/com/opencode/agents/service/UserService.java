package com.opencode.agents.service;

import com.opencode.agents.domain.vo.UserProfileVO;

public interface UserService {

    UserProfileVO getProfile(Long userId);

}
