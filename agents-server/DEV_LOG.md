# 开发进度日志

> 日期：2026-06-05
> 项目：AI智能体小程序（agents-app + agents-server）

---

## 一、当前项目状态

| 组件 | 状态 | 说明 |
|------|------|------|
| 后端 Spring Boot | ✅ 运行中 (localhost:8080) | IDEA 启动，MySQL + Redis |
| 前端 uni-app 3.x | ⚠️ 编译中（见下文） | HBuilderX 运行到微信开发者工具 |
| MySQL | ✅ 已启动 | agents 数据库，root/12345678 |
| Redis | ✅ 已启动 | localhost:6379 |
| 火山引擎豆包 Ark API | ⚠️ 需要配置 | `application-local.yml` 中 `ark-api-key` 是占位值 |

---

## 二、今日完成的工作

### 2.1 核心功能修复（阶段一）

| # | 修复项 | 涉及文件 | 说明 |
|---|--------|----------|------|
| 1 | StreamingText 流式文本重置 Bug | `components/StreamingText.vue` | 流式模式下追加内容而非从头播放 |
| 2 | 会话列表查询逻辑错误 | `ConversationServiceImpl.java` | `agentId=null` 时返回用户所有会话（而非只查 `agentId IS NULL`） |
| 3 | Login 返回缺少 agentCount | `AuthServiceImpl.java` | 登录时统计用户创建的智能体数量 |
| 4 | 创建智能体步骤2&3 空壳 | `AgentServiceImpl.java` | 知识库配置和权限配置接入真实逻辑（创建知识库目录、标记特性） |
| 5 | 前端 SSE 未解析 | `utils/sse-parser.js` + `api/chat.js` + `api/agent.js` | 新增 SSE 解析器，chat 和 create-from-nl 都正确解析流式事件 |
| 6 | 对话后 lastUsedTime 不更新 | `ChatServiceImpl.java` | 每次对话更新 user_agent 最后使用时间 |
| 7 | 流式初始空白气泡 | `components/MessageBubble.vue` | streamingContent 为空时不渲染 |

### 2.2 知识库功能（新实现）

| 文件 | 说明 |
|------|------|
| `manager/FileStorageService.java` | 本地文件存储服务（`uploads/` 目录） |
| `controller/KnowledgeController.java` | 文件上传 / 列表 / 删除 API |
| `controller/FileController.java` | 文件访问 API |
| `mapper/KnowledgeDocMapper.java` | 知识库文档 Mapper |
| `stores/chat.js` | `sendMessage` 增加 `fileIds` 参数 |
| `pages/dialogue/dialogue.vue` | 新增文件附件按钮、文件预览、上传流程 |

### 2.3 后端地址配置化

| 文件 | 说明 |
|------|------|
| `config.js` | 新增 API 地址配置，支持环境变量 `VITE_API_BASE` 覆盖 |
| `request.js` | 使用 `config.js` 的 BASE_URL，新增 `postStream` 函数 |
| 所有硬编码 IP | 已替换为动态配置 |

### 2.4 微信登录修复

| 改动 | 说明 |
|------|------|
| `AuthServiceImpl.code2session()` | 当微信 API 返回业务错误时，降级为 mock 登录（开发模式） |
| 注意 | 后端 **需要重启** 才能生效 |

### 2.5 构建适配

| 改动 | 说明 |
|------|------|
| `vite.config.js`（已移除） | 试验性地创建过，但 HBuilderX 不需要，已删除 |
| `index.html`（已移除） | HBuilderX 小程序模式不需要，已删除 |
| `src/` 目录（已移除） | HBuilderX 需要文件在根目录，已合并到根 |

---

## 三、项目结构（最终）

```
agents-app/
├── manifest.json           # uni-app 配置（mp-weixin appid: wx12751c57e3949d54）
├── pages.json              # 页面路由（dialogue + mine）
├── main.js                 # 入口（Vue3 + Pinia）
├── App.vue                 # 根组件（自动登录）
├── uni.scss                # 全局样式变量
├── config.js               # API_BASE_URL 配置
├── package.json            # 依赖 + 脚本
├── pages/
│   ├── dialogue/dialogue.vue   # 对话页（聊天 + 创建智能体 + 文件上传）
│   └── mine/mine.vue           # 我的页（用户信息 + 调试面板）
├── components/
│   ├── AgentSelector.vue       # 智能体选择器
│   ├── MessageBubble.vue       # 消息气泡
│   ├── StepIndicator.vue       # 创建步骤指示器
│   └── StreamingText.vue       # 流式文本组件（已修复）
├── stores/
│   ├── auth.js                 # 登录态管理
│   ├── agent.js                # 智能体管理
│   └── chat.js                 # 对话管理（支持 fileIds）
├── api/
│   ├── request.js              # HTTP 请求封装（支持 SSE rawResponse）
│   ├── auth.js                 # 登录 API
│   ├── agent.js                # 智能体 API（支持 SSE 解析）
│   └── chat.js                 # 对话 API（支持 SSE 解析）
└── utils/
    └── sse-parser.js           # SSE 事件解析工具

agents-server/
├── src/main/java/com/opencode/agents/
│   ├── controller/
│   │   ├── AuthController.java         # 登录接口
│   │   ├── AgentController.java        # 智能体 CRUD
│   │   ├── ChatController.java         # 对话 + 会话管理
│   │   ├── HealthController.java       # 健康检查
│   │   ├── KnowledgeController.java    # 🆕 文件上传管理
│   │   └── FileController.java         # 🆕 文件访问
│   ├── service/
│   │   └── impl/
│   │       ├── AuthServiceImpl.java    # 登录（微信 + mock 降级）
│   │       ├── AgentServiceImpl.java   # 创建智能体（步骤2&3补全）
│   │       ├── ChatServiceImpl.java    # 流式对话（文件上下文 + lastUsedTime）
│   │       └── ConversationServiceImpl.java  # 会话列表（查询修复）
│   ├── manager/
│   │   ├── AiManager.java             # 豆包 LLM 封装
│   │   ├── SseManager.java            # SSE 流式管理
│   │   └── FileStorageService.java    # 🆕 文件存储服务
│   └── mapper/
│       └── KnowledgeDocMapper.java    # 🆕 知识库文档 Mapper
└── src/main/resources/
    ├── application.yml                # 通用配置（含 multipart）
    └── application-local.yml          # 本地配置（数据库/微信/火山引擎）
```

---

## 四、待解决的问题

### 🔴 紧急

| 问题 | 说明 | 优先级 |
|------|------|--------|
| 后端未重启 | IDEA 中的后端还是旧代码，登录 mock 修复未生效 | 🔴 明天先做 |
| 火山引擎 API Key 未配置 | `application-local.yml` 中 `ark-api-key` 是占位值，AI 对话会失败 | 🔴 需填入真实 key |
| 文件上传真实实现 | `FileStorageService` 用本地文件存储，生产需接入阿里云 OSS | 🔴 看需要 |

### 🟠 阶段二（架构加固）

| 问题 | 说明 |
|------|------|
| 分页支持 | 所有 list 接口（agents/conversations/messages）缺少分页 |
| 开发模式多用户 | mock 登录所有用户共用 `dev_fallback_user`，可改为随机 UUID |
| 模型名称配置化 | `AiManager.java` 中 `doubao-1.5-vision-pro-250328` 硬编码 |
| 限流 | 接口无频率限制 |

### 🟡 体验优化

| 问题 | 说明 |
|------|------|
| 对话标题 AI 自动生成 | 目前是截取前30字 |
| 前端骨架屏 | 页面加载时无 loading |
| 前端错误处理 | 对话失败重试机制 |
| 页面间通信 | 创建智能体后切换到对话页自动选中 |

---

## 五、明天继续的入口

1. **重启后端** → IDEA 中重启 `AgentsApplication`
2. **配置 Ark API Key** → 在 `application-local.yml` 中填入真实 key
3. **验证完整流程** → 登录 → 创建智能体 → 对话 → 文件上传 → 智能体管理
4. **推进阶段二** → 分页 / 多用户mock / 模型配置化
