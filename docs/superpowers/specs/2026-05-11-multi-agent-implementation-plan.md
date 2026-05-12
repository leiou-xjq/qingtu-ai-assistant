# 多 Agent 架构实现计划

**日期**: 2026-05-11
**版本**: v1.0
**基于设计**: 2026-05-11-multi-agent-design.md

---

## Phase 1: 基础设施（2-3天）

### 1.1 消息队列配置

**文件**: `com/qingtu/agent/agent/message/config/AgentQueueConfig.java`
```java
@Configuration
public class AgentQueueConfig {
    // 队列定义
    public static final String QUEUE_WEATHER = "agent.weather";
    public static final String QUEUE_EXPENSE = "agent.expense";
    public static final String QUEUE_COURSE = "agent.course";
    public static final String QUEUE_PROFILE = "agent.profile";
    public static final String QUEUE_NOTE = "agent.note";
    public static final String QUEUE_RESULTS = "agent.results";

    // Exchange 定义
    public static final String EXCHANGE_AGENT = "agent.exchange";
    public static final String EXCHANGE_RESULTS = "agent.results.exchange";
}
```

**任务**:
- [ ] 配置 RabbitMQ 队列和交换机
- [ ] 定义 TaskMessage 和 ResultMessage 消息类
- [ ] 创建消息发布者 AgentMessagePublisher
- [ ] 创建消息监听器基类 AgentListener

### 1.2 用户上下文提供者

**文件**: `com/qingtu/agent/agent/orchestrator/UserContextProvider.java`

**任务**:
- [ ] 创建 UserContext 类
- [ ] 从 JWT 解析用户信息
- [ ] 提供 getContext(token) 方法

### 1.3 降级处理器

**文件**: `com/qingtu/agent/agent/fallback/FallbackHandler.java`

**任务**:
- [ ] 实现各 Agent 的降级方法
- [ ] 集成 LLM 降级预测

---

## Phase 2: Orchestrator 核心（2-3天）

### 2.1 意图分析器

**文件**: `com/qingtu/agent/agent/orchestrator/IntentAnalyzer.java`

**Prompt 模板**:
```markdown
你是一个意图分析器，分析用户消息并分解任务。

可用Agent:
- weather: 天气查询
- expense: 记账创建
- course: 课程导入
- profile: 个人信息修改
- note: 笔记生成

用户消息: {message}
文件: {files}

输出JSON:
{
  "intent": "expense_create",
  "parameters": {...},
  "requiresFiles": true/false
}
```

**任务**:
- [ ] 创建 IntentAnalyzer 类
- [ ] 编写意图分析 Prompt
- [ ] 调用 LLM 分析意图
- [ ] 返回 Task 对象列表

### 2.2 任务分解器

**文件**: `com/qingtu/agent/agent/orchestrator/TaskDecomposer.java`

**任务**:
- [ ] 将 IntentResult 转换为 Task 列表
- [ ] 处理多任务并行场景
- [ ] 任务依赖关系处理

### 2.3 结果聚合器

**文件**: `com/qingtu/agent/agent/orchestrator/ResultAggregator.java`

**任务**:
- [ ] 收集各 Agent 返回结果
- [ ] 处理部分成功场景
- [ ] 生成友好回复

### 2.4 编排器主类

**文件**: `com/qingtu/agent/agent/orchestrator/OrchestratorAgent.java`

**任务**:
- [ ] 整合 IntentAnalyzer、TaskDecomposer、ResultAggregator
- [ ] 实现消息发布逻辑
- [ ] 实现结果收集逻辑

---

## Phase 3: Specialist Agents（3-5天）

### 3.1 WeatherAgent

**文件**: `com/qingtu/agent/agent/specialist/WeatherAgent.java`

**任务**:
- [ ] 监听 queue.weather
- [ ] 调用 WeatherQueryTool
- [ ] 处理降级逻辑

### 3.2 ExpenseAgent

**文件**: `com/qingtu/agent/agent/specialist/ExpenseAgent.java`

**任务**:
- [ ] 监听 queue.expense
- [ ] 创建 Expense 记录
- [ ] 处理降级逻辑

### 3.3 CourseAgent

**文件**: `com/qingtu/agent/agent/specialist/CourseAgent.java`

**任务**:
- [ ] 监听 queue.course
- [ ] 文件 Base64 解码
- [ ] 调用 ParseJobService
- [ ] 处理降级逻辑

### 3.4 ProfileAgent

**文件**: `com/qingtu/agent/agent/specialist/ProfileAgent.java`

**任务**:
- [ ] 监听 queue.profile
- [ ] 修改 User 表
- [ ] 处理降级逻辑

### 3.5 NoteAgent

**文件**: `com/qingtu/agent/agent/specialist/NoteAgent.java`

**任务**:
- [ ] 监听 queue.note
- [ ] 调用 LLM 生成笔记
- [ ] 保存到 notes 表

---

## Phase 4: API 集成（1-2天）

### 4.1 对话接口

**文件**: `com/qingtu/agent/controller/AgentController.java`

**任务**:
- [ ] POST /api/agent/chat (支持文件上传)
- [ ] GET /api/agent/tasks/{taskId}
- [ ] 集成 OrchestratorAgent

---

## Phase 5: 测试与优化（2天）

### 5.1 单元测试

**任务**:
- [ ] IntentAnalyzer 测试
- [ ] 各 Agent 单元测试
- [ ] 降级策略测试

### 5.2 集成测试

**任务**:
- [ ] 端到端对话测试
- [ ] 多 Agent 并行测试
- [ ] RabbitMQ 消息流测试

---

## 依赖关系

```
Phase 1 (基础设施)
    ↓
Phase 2 (Orchestrator)
    ↓
Phase 3 (Agents) ← 依赖 Phase 1
    ↓
Phase 4 (API) ← 依赖 Phase 2, 3
    ↓
Phase 5 (测试)
```

---

## 预估工期

- Phase 1: 2-3 天
- Phase 2: 2-3 天
- Phase 3: 3-5 天
- Phase 4: 1-2 天
- Phase 5: 2 天

**总计**: 10-15 天

---

## 关键风险

1. **RabbitMQ 连接池耗尽**: 限制消费者并发数
2. **大文件 Base64 超时**: 实现分片上传
3. **意图分析不准确**: 持续优化 Prompt

---

**计划状态**: 待执行
**下一步**: 开始 Phase 1