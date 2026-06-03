package com.opencode.agents.service;

import com.opencode.agents.domain.vo.AgentDetailVO;
import com.opencode.agents.domain.vo.AgentVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AgentService {

    SseEmitter createFromNL(Long userId, String description);

    SseEmitter updateFromNL(Long userId, Long agentId, String description);

    List<AgentVO> listMyAgents(Long userId);

    AgentDetailVO getAgentDetail(Long agentId, Long userId);

    void deleteAgent(Long agentId, Long userId);

}
