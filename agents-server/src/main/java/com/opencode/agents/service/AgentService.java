package com.opencode.agents.service;

import com.opencode.agents.domain.dto.CreateAgentRequest;
import com.opencode.agents.domain.dto.UpdateAgentRequest;
import com.opencode.agents.domain.vo.AgentDetailVO;
import com.opencode.agents.domain.vo.AgentVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AgentService {

    SseEmitter createFromNL(Long userId, String description);

    SseEmitter updateFromNL(Long userId, Long agentId, String description);

    /**
     * 结构化创建智能体（手动表单）
     */
    AgentVO createAgent(Long userId, CreateAgentRequest request);

    /**
     * 更新智能体设置
     */
    AgentVO updateAgent(Long agentId, Long userId, UpdateAgentRequest request);

    List<AgentVO> listMyAgents(Long userId);

    AgentDetailVO getAgentDetail(Long agentId, Long userId);

    void deleteAgent(Long agentId, Long userId);

}
