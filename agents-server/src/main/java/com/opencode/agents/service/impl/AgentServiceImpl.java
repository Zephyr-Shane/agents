package com.opencode.agents.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.AgentVersion;
import com.opencode.agents.domain.entity.UserAgent;
import com.opencode.agents.domain.vo.AgentDetailVO;
import com.opencode.agents.domain.vo.AgentVO;
import com.opencode.agents.manager.AgentPromptConstants;
import com.opencode.agents.manager.AiManager;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.AgentVersionMapper;
import com.opencode.agents.mapper.UserAgentMapper;
import com.opencode.agents.service.AgentService;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentVersionMapper agentVersionMapper;
    private final UserAgentMapper userAgentMapper;
    private final AiManager aiManager;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public SseEmitter createFromNL(Long userId, String description) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executor.execute(() -> {
            try {
                // Step 1: 正在解析角色定位
                sendStep(emitter, 1, 4, "analyzing", "正在解析角色定位...");

                // 调用 LLM 解析自然语言
                ChatMessage systemMsg = ChatMessage.builder()
                        .role(ChatMessageRole.SYSTEM)
                        .content(AgentPromptConstants.NL_AGENT_CREATOR_SYSTEM_PROMPT)
                        .build();
                ChatMessage userMsg = ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(description)
                        .build();

                String llmResult = aiManager.syncChat(List.of(systemMsg, userMsg));
                if (llmResult == null) {
                    sendError(emitter, "AI解析失败，请重试");
                    return;
                }

                // 解析 JSON
                JSONObject config = parseAgentConfig(llmResult);
                if (config == null) {
                    sendError(emitter, "配置解析失败，请调整描述后重试");
                    return;
                }

                // Step 2: 正在配置知识库能力
                sendStep(emitter, 2, 4, "configuring", "正在配置知识库能力...");

                // Step 3: 正在配置联网权限
                sendStep(emitter, 3, 4, "permissions", "正在配置联网权限...");

                // 保存到数据库
                Agent agent = saveAgent(userId, config);

                // Step 4: 完成
                sendStepWithData(emitter, 4, 4, "completed", "智能体创建完成", agent.getId());

                emitter.complete();

            } catch (Exception e) {
                log.error("智能体创建异常", e);
                sendError(emitter, "创建失败: " + e.getMessage());
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter updateFromNL(Long userId, Long agentId, String description) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executor.execute(() -> {
            try {
                Agent agent = agentMapper.selectById(agentId);
                if (agent == null || !agent.getCreatorId().equals(userId)) {
                    sendError(emitter, "智能体不存在或无权限修改");
                    return;
                }

                // 获取当前版本配置
                AgentVersion currentVersion = agentVersionMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentVersion>()
                                .eq(AgentVersion::getAgentId, agentId)
                                .eq(AgentVersion::getVersion, agent.getCurrentVersion())
                );

                String currentConfigJson = currentVersion != null ? currentVersion.getFeaturesJson() : "{}";

                String updatePrompt = AgentPromptConstants.NL_AGENT_UPDATE_SYSTEM_PROMPT
                        .replace("{currentConfig}", currentConfigJson)
                        .replace("{userRequest}", description);

                ChatMessage systemMsg = ChatMessage.builder()
                        .role(ChatMessageRole.SYSTEM)
                        .content(updatePrompt)
                        .build();
                ChatMessage userMsg = ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(description)
                        .build();

                String llmResult = aiManager.syncChat(List.of(systemMsg, userMsg));
                if (llmResult == null) {
                    sendError(emitter, "AI解析失败");
                    return;
                }

                JSONObject config = parseAgentConfig(llmResult);
                if (config == null) {
                    sendError(emitter, "配置解析失败");
                    return;
                }

                // 创建新版本
                int newVersion = agent.getCurrentVersion() + 1;
                String changeLog = config.getStr("changeLog", "自然语言修改");

                AgentVersion version = new AgentVersion();
                version.setAgentId(agentId);
                version.setVersion(newVersion);
                version.setSystemPrompt(config.getStr("systemPrompt"));
                version.setFeaturesJson(config.get("features").toString());
                version.setDescription(config.getStr("description"));
                version.setChangeLog(changeLog);
                version.setCreatedBy(userId);
                agentVersionMapper.insert(version);

                // 更新 agent 当前版本
                agent.setName(config.getStr("name"));
                agent.setDescription(config.getStr("description"));
                agent.setCurrentVersion(newVersion);
                agentMapper.updateById(agent);

                sendStepWithData(emitter, 1, 1, "completed", "智能体更新完成", agentId);
                emitter.complete();

            } catch (Exception e) {
                log.error("智能体更新异常", e);
                sendError(emitter, "更新失败: " + e.getMessage());
            }
        });

        return emitter;
    }

    @Override
    public List<AgentVO> listMyAgents(Long userId) {
        return agentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Agent>()
                        .eq(Agent::getCreatorId, userId)
                        .eq(Agent::getDeleted, 0)
                        .orderByDesc(Agent::getCreateTime)
        ).stream().map(this::toAgentVO).collect(Collectors.toList());
    }

    @Override
    public AgentDetailVO getAgentDetail(Long agentId, Long userId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !agent.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "智能体不存在");
        }

        AgentVersion version = agentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentVersion>()
                        .eq(AgentVersion::getAgentId, agentId)
                        .eq(AgentVersion::getVersion, agent.getCurrentVersion())
        );

        AgentDetailVO vo = new AgentDetailVO();
        vo.setId(agent.getId());
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setAvatar(agent.getAvatar());
        vo.setCurrentVersion(agent.getCurrentVersion());
        vo.setSystemPrompt(version != null ? version.getSystemPrompt() : "");
        vo.setFeaturesJson(version != null ? version.getFeaturesJson() : "{}");
        vo.setCreateTime(agent.getCreateTime());
        return vo;
    }

    @Override
    @Transactional
    public void deleteAgent(Long agentId, Long userId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !agent.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "智能体不存在");
        }
        agentMapper.deleteById(agentId);
    }

    @Transactional
    protected Agent saveAgent(Long userId, JSONObject config) {
        Agent agent = new Agent();
        agent.setCreatorId(userId);
        agent.setName(config.getStr("name"));
        agent.setDescription(config.getStr("description"));
        agent.setAvatar("");
        agent.setCurrentVersion(1);
        agent.setStatus(1);
        agent.setIsPublic(0);
        agentMapper.insert(agent);

        AgentVersion version = new AgentVersion();
        version.setAgentId(agent.getId());
        version.setVersion(1);
        version.setSystemPrompt(config.getStr("systemPrompt"));
        JSONObject features = config.getJSONObject("features");
        version.setFeaturesJson(features != null ? features.toString() : "{}");
        version.setDescription(config.getStr("description"));
        version.setChangeLog("初始创建");
        version.setCreatedBy(userId);
        agentVersionMapper.insert(version);

        UserAgent userAgent = new UserAgent();
        userAgent.setUserId(userId);
        userAgent.setAgentId(agent.getId());
        userAgent.setIsCreator(1);
        userAgent.setIsFavorite(0);
        userAgent.setFirstUsedTime(LocalDateTime.now());
        userAgent.setLastUsedTime(LocalDateTime.now());
        userAgentMapper.insert(userAgent);

        return agent;
    }

    private JSONObject parseAgentConfig(String llmOutput) {
        try {
            // 清理可能的 markdown 代码块标记
            String cleaned = llmOutput
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            return JSONUtil.parseObj(cleaned);
        } catch (Exception e) {
            log.warn("解析Agent配置JSON失败: {}", llmOutput, e);
            return null;
        }
    }

    private void sendStep(SseEmitter emitter, int step, int total, String action, String message) {
        sendSseEvent(emitter, "{\"type\":\"step\",\"step\":" + step +
                ",\"total\":" + total +
                ",\"action\":\"" + action +
                "\",\"message\":\"" + message + "\"}");
    }

    private void sendStepWithData(SseEmitter emitter, int step, int total, String action, String message, Long agentId) {
        sendSseEvent(emitter, "{\"type\":\"step\",\"step\":" + step +
                ",\"total\":" + total +
                ",\"action\":\"" + action +
                "\",\"message\":\"" + message +
                "\",\"agentId\":" + agentId + "}");
    }

    private void sendError(SseEmitter emitter, String message) {
        sendSseEvent(emitter, "{\"type\":\"error\",\"content\":\"" + message + "\"}");
        try {
            emitter.complete();
        } catch (Exception ignored) {}
    }

    private void sendSseEvent(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().name("message").data(data));
        } catch (IOException e) {
            log.warn("SSE发送失败", e);
        }
    }

    private AgentVO toAgentVO(Agent agent) {
        AgentVO vo = new AgentVO();
        vo.setId(agent.getId());
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setAvatar(agent.getAvatar());
        vo.setCreateTime(agent.getCreateTime());
        return vo;
    }

}
