package com.opencode.agents.controller;

import com.opencode.agents.common.UserContext;
import com.opencode.agents.domain.dto.CreateAgentRequest;
import com.opencode.agents.domain.dto.UpdateAgentRequest;
import com.opencode.agents.domain.vo.AgentDetailVO;
import com.opencode.agents.domain.vo.AgentVO;
import com.opencode.agents.domain.vo.ResultVO;
import com.opencode.agents.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public ResultVO<List<AgentVO>> listMyAgents() {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.listMyAgents(userId));
    }

    @GetMapping("/{id}")
    public ResultVO<AgentDetailVO> getAgentDetail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.getAgentDetail(id, userId));
    }

    @PostMapping("/create-from-nl")
    public SseEmitter createFromNL(@RequestBody CreateAgentRequest request) {
        Long userId = UserContext.getUserId();
        return agentService.createFromNL(userId, request.getDescription());
    }

    @PostMapping("/update-from-nl")
    public SseEmitter updateFromNL(@RequestBody UpdateAgentRequest request) {
        Long userId = UserContext.getUserId();
        return agentService.updateFromNL(userId, request.getAgentId(), request.getDescription());
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> deleteAgent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        agentService.deleteAgent(id, userId);
        return ResultVO.success();
    }

}
