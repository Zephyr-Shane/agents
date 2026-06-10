package com.opencode.agents.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.domain.dto.CreateAgentRequest;
import com.opencode.agents.domain.dto.UpdateAgentRequest;
import com.opencode.agents.domain.entity.Agent;
import com.opencode.agents.domain.entity.AgentVersion;
import com.opencode.agents.domain.entity.KnowledgeDoc;
import com.opencode.agents.domain.entity.UserAgent;
import com.opencode.agents.domain.vo.AgentDetailVO;
import com.opencode.agents.domain.vo.AgentVO;
import com.opencode.agents.manager.AgentPromptConstants;
import com.opencode.agents.manager.AiManager;
import com.opencode.agents.manager.FileStorageService;
import com.opencode.agents.mapper.AgentMapper;
import com.opencode.agents.mapper.AgentVersionMapper;
import com.opencode.agents.mapper.KnowledgeDocMapper;
import com.opencode.agents.mapper.UserAgentMapper;
import com.opencode.agents.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private final KnowledgeDocMapper knowledgeDocMapper;
    private final AiManager aiManager;
    private final FileStorageService fileStorageService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public SseEmitter createFromNL(Long userId, String description) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        executor.execute(() -> {
            try {
                sendStep(emitter, 1, 4, "analyzing", "正在解析角色定位...");

                List<Message> messages = List.of(
                        new SystemMessage(AgentPromptConstants.NL_AGENT_CREATOR_SYSTEM_PROMPT),
                        new UserMessage(description)
                );

                var chatResponse = aiManager.syncChat(messages);
                String llmResult = chatResponse != null && chatResponse.getResult() != null
                        && chatResponse.getResult().getOutput() != null
                        ? chatResponse.getResult().getOutput().getText() : null;
                if (llmResult == null) {
                    sendError(emitter, "AI解析失败，请重试");
                    return;
                }

                JSONObject config = parseAgentConfig(llmResult);
                if (config == null) {
                    sendError(emitter, "配置解析失败，请调整描述后重试");
                    return;
                }

                JSONObject features = config.getJSONObject("features");
                boolean hasKnowledgeBase = features != null && features.getBool("knowledgeBase", false);
                if (hasKnowledgeBase) {
                    String kbDir = "kb/" + userId + "/" + config.getStr("name", "agent");
                    Files.createDirectories(
                            Paths.get(fileStorageService.getFilePath("").getParent().toString(), kbDir)
                    );
                    String readmeContent = "知识库目录 - " + config.getStr("name") + "\n"
                            + "创建时间: " + LocalDateTime.now() + "\n"
                            + "通过上方知识库管理上传文档，即可在对话中引用知识库内容。";
                    Files.writeString(
                            Paths.get(fileStorageService.getFilePath("").getParent().toString(), kbDir, "README.md"),
                            readmeContent
                    );
                    log.info("创建知识库目录: agent={}, dir={}", config.getStr("name"), kbDir);
                }
                sendStep(emitter, 2, 4, "configuring", hasKnowledgeBase
                        ? "知识库能力已配置" : "未检测到知识库需求，已跳过");

                boolean hasWebSearch = features != null && features.getBool("webSearch", false);
                boolean hasCodeInterpreter = features != null && features.getBool("codeInterpreter", false);
                boolean hasFileUpload = features != null && features.getBool("fileUpload", false);

                StringBuilder permMsg = new StringBuilder("权限配置完成");
                if (hasWebSearch) permMsg.append(" [联网搜索]");
                if (hasCodeInterpreter) permMsg.append(" [代码执行]");
                if (hasFileUpload) permMsg.append(" [文件上传]");
                if (!hasWebSearch && !hasCodeInterpreter && !hasFileUpload) {
                    permMsg.append("（基础权限）");
                }

                if (features != null) {
                    features.set("knowledgeBase", hasKnowledgeBase);
                    features.set("webSearch", hasWebSearch);
                    features.set("codeInterpreter", hasCodeInterpreter);
                    features.set("fileUpload", hasFileUpload);
                }

                sendStep(emitter, 3, 4, "permissions", permMsg.toString());

                Agent agent = saveAgent(userId, config);

                sendSseEvent(emitter, "{\"type\":\"completed\",\"agentId\":" + agent.getId()
                        + ",\"name\":\"" + agent.getName()
                        + "\",\"message\":\"智能体创建完成\"}");
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

                AgentVersion currentVersion = agentVersionMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentVersion>()
                                .eq(AgentVersion::getAgentId, agentId)
                                .eq(AgentVersion::getVersion, agent.getCurrentVersion())
                );

                String currentConfigJson = currentVersion != null ? currentVersion.getFeaturesJson() : "{}";

                String updatePrompt = AgentPromptConstants.NL_AGENT_UPDATE_SYSTEM_PROMPT
                        .replace("{currentConfig}", currentConfigJson)
                        .replace("{userRequest}", description);

                List<Message> messages = List.of(
                        new SystemMessage(updatePrompt),
                        new UserMessage(description)
                );

                var chatResponse = aiManager.syncChat(messages);
                String llmResult = chatResponse != null && chatResponse.getResult() != null
                        && chatResponse.getResult().getOutput() != null
                        ? chatResponse.getResult().getOutput().getText() : null;
                if (llmResult == null) {
                    sendError(emitter, "AI解析失败");
                    return;
                }

                JSONObject config = parseAgentConfig(llmResult);
                if (config == null) {
                    sendError(emitter, "配置解析失败");
                    return;
                }

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

                agent.setName(config.getStr("name"));
                agent.setDescription(config.getStr("description"));
                agent.setCurrentVersion(newVersion);
                agentMapper.updateById(agent);

                sendSseEvent(emitter, "{\"type\":\"completed\",\"agentId\":" + agentId
                        + ",\"name\":\"" + agent.getName()
                        + "\",\"message\":\"智能体更新完成\"}");
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
    @Transactional
    public AgentVO createAgent(Long userId, CreateAgentRequest request) {
        String name = request.getName();
        String description = request.getAgentDescription();
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "智能体名称不能为空");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "功能描述不能为空");
        }

        String type = request.getType();
        if (type == null || type.isBlank()) {
            type = "general";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个AI智能体，请以设定的角色与用户对话。\n\n");
        sb.append("## 智能体名称\n").append(name).append("\n\n");
        sb.append("## 功能描述\n").append(description).append("\n\n");
        if (request.getIntroduction() != null && !request.getIntroduction().isBlank()) {
            sb.append("## 介绍\n").append(request.getIntroduction()).append("\n\n");
        }
        if (request.getOpeningLine() != null && !request.getOpeningLine().isBlank()) {
            sb.append("## 开场白\n首次对话时，先发送开场白：").append(request.getOpeningLine()).append("\n\n");
        }
        String systemPrompt = sb.toString();

        Agent agent = new Agent();
        agent.setCreatorId(userId);
        agent.setName(name);
        agent.setDescription(description);
        agent.setIntroduction(request.getIntroduction() != null ? request.getIntroduction() : "");
        agent.setOpeningLine(request.getOpeningLine() != null ? request.getOpeningLine() : "");
        agent.setType(type);
        agent.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
        agent.setCurrentVersion(1);
        agent.setStatus(1);
        agent.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 0);
        agentMapper.insert(agent);

        AgentVersion version = new AgentVersion();
        version.setAgentId(agent.getId());
        version.setVersion(1);
        version.setSystemPrompt(systemPrompt);
        version.setFeaturesJson("{\"type\":\"" + type + "\"}");
        version.setDescription(description);
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

        log.info("结构化创建智能体: userId={}, name={}, type={}", userId, name, type);
        return toAgentVO(agent);
    }

    @Override
    @Transactional
    public AgentVO updateAgent(Long agentId, Long userId, UpdateAgentRequest request) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !agent.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "智能体不存在或无权限修改");
        }

        boolean changed = false;
        if (request.getName() != null && !request.getName().isBlank()) {
            agent.setName(request.getName()); changed = true;
        }
        if (request.getAgentDescription() != null && !request.getAgentDescription().isBlank()) {
            agent.setDescription(request.getAgentDescription()); changed = true;
        }
        if (request.getIntroduction() != null) {
            agent.setIntroduction(request.getIntroduction()); changed = true;
        }
        if (request.getOpeningLine() != null) {
            agent.setOpeningLine(request.getOpeningLine()); changed = true;
        }
        if (request.getAvatar() != null) {
            agent.setAvatar(request.getAvatar()); changed = true;
        }
        if (request.getIsPublic() != null) {
            agent.setIsPublic(request.getIsPublic()); changed = true;
        }

        if (!changed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有要修改的内容");
        }

        int newVersion = agent.getCurrentVersion() + 1;
        agent.setCurrentVersion(newVersion);
        agentMapper.updateById(agent);

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个AI智能体，请以设定的角色与用户对话。\n\n");
        sb.append("## 智能体名称\n").append(agent.getName()).append("\n\n");
        sb.append("## 功能描述\n").append(agent.getDescription()).append("\n\n");
        if (agent.getIntroduction() != null && !agent.getIntroduction().isBlank()) {
            sb.append("## 介绍\n").append(agent.getIntroduction()).append("\n\n");
        }
        if (agent.getOpeningLine() != null && !agent.getOpeningLine().isBlank()) {
            sb.append("## 开场白\n首次对话时，先发送开场白：").append(agent.getOpeningLine()).append("\n\n");
        }
        String systemPrompt = sb.toString();

        AgentVersion version = new AgentVersion();
        version.setAgentId(agentId);
        version.setVersion(newVersion);
        version.setSystemPrompt(systemPrompt);
        version.setFeaturesJson("{\"type\":\"" + agent.getType() + "\"}");
        version.setDescription(agent.getDescription());
        version.setChangeLog("手动更新");
        version.setCreatedBy(userId);
        agentVersionMapper.insert(version);

        log.info("更新智能体: agentId={}, name={}, newVersion={}", agentId, agent.getName(), newVersion);
        return toAgentVO(agent);
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
        vo.setIntroduction(agent.getIntroduction());
        vo.setOpeningLine(agent.getOpeningLine());
        vo.setAvatar(agent.getAvatar());
        vo.setCurrentVersion(agent.getCurrentVersion());
        vo.setType(agent.getType());
        vo.setIsPublic(agent.getIsPublic());
        vo.setSystemPrompt(version != null ? version.getSystemPrompt() : "");
        vo.setFeaturesJson(version != null ? version.getFeaturesJson() : "{}");
        vo.setCreateTime(agent.getCreateTime());
        return vo;
    }

    @Override
    @Transactional
    public AgentVO rollbackAgent(Long agentId, Long userId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !agent.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "智能体不存在或无权限");
        }

        int currentVer = agent.getCurrentVersion();
        if (currentVer <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "没有可回滚的版本");
        }

        AgentVersion currentVersion = agentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentVersion>()
                        .eq(AgentVersion::getAgentId, agentId)
                        .eq(AgentVersion::getVersion, currentVer)
        );

        if (currentVersion != null && currentVersion.getRollbackSourceVersion() != null
                && currentVersion.getRollbackSourceVersion() > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前版本已是回滚结果，不可连续回滚");
        }

        AgentVersion prevVersion = agentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentVersion>()
                        .eq(AgentVersion::getAgentId, agentId)
                        .eq(AgentVersion::getVersion, currentVer - 1)
        );
        if (prevVersion == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上一版本不存在，无法回滚");
        }

        int newVersion = currentVer + 1;
        AgentVersion rollbackVersion = new AgentVersion();
        rollbackVersion.setAgentId(agentId);
        rollbackVersion.setVersion(newVersion);
        rollbackVersion.setSystemPrompt(prevVersion.getSystemPrompt());
        rollbackVersion.setFeaturesJson(prevVersion.getFeaturesJson());
        rollbackVersion.setDescription(prevVersion.getDescription());
        rollbackVersion.setChangeLog("版本回滚 (v" + currentVer + " → v" + (currentVer - 1) + ")");
        rollbackVersion.setCreatedBy(userId);
        rollbackVersion.setRollbackSourceVersion(currentVer);
        agentVersionMapper.insert(rollbackVersion);

        agent.setCurrentVersion(newVersion);
        agentMapper.updateById(agent);

        log.info("智能体版本回滚: agentId={}, from=v{}, to=v{}, newVersion=v{}",
                agentId, currentVer, currentVer - 1, newVersion);
        return toAgentVO(agent);
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
        agent.setIntroduction(config.getStr("introduction", ""));
        agent.setOpeningLine(config.getStr("openingLine", ""));
        agent.setType("general");
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
        try { emitter.complete(); } catch (Exception ignored) {}
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
        vo.setIntroduction(agent.getIntroduction());
        vo.setOpeningLine(agent.getOpeningLine());
        vo.setAvatar(agent.getAvatar());
        vo.setType(agent.getType());
        vo.setIsPublic(agent.getIsPublic());
        vo.setCreateTime(agent.getCreateTime());
        return vo;
    }

}
