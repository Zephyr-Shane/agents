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

    /**
     * 获取我的智能体列表
     */
    @GetMapping
    public ResultVO<List<AgentVO>> listMyAgents() {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.listMyAgents(userId));
    }

    /**
     * 获取智能体详情
     */
    @GetMapping("/{id}")
    public ResultVO<AgentDetailVO> getAgentDetail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.getAgentDetail(id, userId));
    }

    /**
     * 自然语言创建智能体（SSE 流式）
     */
    @PostMapping("/create-from-nl")
    public SseEmitter createFromNL(@RequestBody CreateAgentRequest request) {
        Long userId = UserContext.getUserId();
        return agentService.createFromNL(userId, request.getDescription());
    }

    /**
     * 结构化创建智能体（手动填写表单）
     */
    @PostMapping
    public ResultVO<AgentVO> createAgent(@RequestBody CreateAgentRequest request) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.createAgent(userId, request));
    }

    /**
     * 更新智能体设置
     */
    @PutMapping("/{id}")
    public ResultVO<AgentVO> updateAgent(@PathVariable Long id, @RequestBody UpdateAgentRequest request) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.updateAgent(id, userId, request));
    }

    /**
     * 自然语言更新智能体（SSE 流式）
     */
    @PostMapping("/update-from-nl")
    public SseEmitter updateFromNL(@RequestBody UpdateAgentRequest request) {
        Long userId = UserContext.getUserId();
        return agentService.updateFromNL(userId, request.getAgentId(), request.getDescription());
    }

    /**
     * 回滚智能体到上一版本（仅允许回滚一级，不可连续回滚）
     */
    @PostMapping("/{id}/rollback")
    public ResultVO<AgentVO> rollbackAgent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ResultVO.success(agentService.rollbackAgent(id, userId));
    }

    /**
     * 删除智能体
     */
    @DeleteMapping("/{id}")
    public ResultVO<Void> deleteAgent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        agentService.deleteAgent(id, userId);
        return ResultVO.success();
    }

}
