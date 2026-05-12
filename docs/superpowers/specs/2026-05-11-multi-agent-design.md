# 多 Agent 架构设计文档

**日期**: 2026-05-11
**版本**: v1.0
**状态**: 待审核

---

## 1. 背景与目标

### 1.1 当前问题
- 青途 AI 助手目前只是简单的对话系统
- 工具需要手动触发，无法自动调用
- 不支持文件/图片上传和智能解析
- 各功能模块耦合度高，难以扩展

### 1.2 升级目标
- 实现真正的 Agent 架构，大模型能自动调用工具
- 支持对话中上传文件/图片，智能解析
- 多工具并行执行，自动保存
- 用户上下文自动注入（登录用户信息）

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Frontend (用户界面)                          │
│   - 对话输入 + 文件上传                                               │
│   - WebSocket 实时推送                                                │
└─────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Orchestrator (编排器)                           │
│   - 管理对话上下文 (按用户ID隔离)                                      │
│   - 意图分析 + 任务分解                                               │
│   - 调用 Specialist Agents                                           │
│   - 结果聚合 + 返回                                                  │
└─────────────────────────────────────────────────────────────────────┘
            │                    │                    │
            ▼                    ▼                    ▼
    ┌───────────┬────────┬───────┴───────┬────────┬───────────┐
    │ Weather   │ Expense│   Course     │ Profile│   Note    │
    │ Agent     │ Agent  │   Agent      │ Agent  │   Agent   │
    └───────────┴────────┴──────────────┴────────┴───────────┘
           │            │              │            │
           └────────────┴──────────────┴────────────┘
                          │
                          ▼
                  ┌───────────────┐
                  │   RabbitMQ    │
                  │   (消息总线)  │
                  └───────────────┘
```

### 2.2 通信方式
- **Orchestrator → Agents**: RabbitMQ (生产者发布任务)
- **Agents → Orchestrator**: RabbitMQ (消费者返回结果)
- **执行模式**: 并行执行，互不阻塞

---

## 3. 核心组件

### 3.1 Orchestrator (编排器)

**职责**:
1. 接收用户消息（含文件 Base64）
2. 从 JWT 解析用户上下文（userId, city, school, semesterStart）
3. 调用 LLM 分析意图，分解任务
4. 发布任务到 RabbitMQ
5. 等待结果，聚合返回

**关键类**:
```java
public class OrchestratorAgent {
    // 用户上下文
    UserContext getContext(String token);

    // 意图分析
    IntentResult analyzeIntent(String message, List<FileData> files);

    // 任务分解
    List<Task> decompose(IntentResult intent);

    // 任务发布
    void publishTasks(List<Task> tasks);

    // 结果聚合
    String aggregateResults(List<TaskResult> results);
}
```

### 3.2 Specialist Agents (领域专家)

| Agent | 职责 | 工具能力 |
|-------|------|---------|
| WeatherAgent | 天气查询 | weather_query |
| ExpenseAgent | 记账创建 | expense_create, expense_query |
| CourseAgent | 课程解析+导入 | file_upload, course_import, course_query |
| ProfileAgent | 个人信息修改 | profile_query, profile_update |
| NoteAgent | 笔记生成 | note_generate, note_query |

### 3.3 用户上下文

```java
public class UserContext {
    Long userId;           // 用户ID
    String city;           // 城市（天气用）
    String school;         // 学校（课程用）
    String semesterStart;  // 学期开始（课程解析用）
    String nickname;      // 昵称
    Double height;         // 身高
    Double weight;         // 体重
}
```

---

## 4. 执行流程

### 4.1 完整流程

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 用户发送消息（含文件）                                          │
│    POST /api/chat/message                                        │
│    {                                                             │
│      "message": "帮我记账，花了25元",                             │
│      "files": [{"name": "receipt.jpg", "data": "base64..."}]     │
│    }                                                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Orchestrator 解析                                              │
│    - JWT → userId = 5, city = "汉中", school = "陕西理工大学"      │
│    - 文件 → 存储到临时目录，返回路径                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. 意图分析 (LLM)                                                 │
│    输入: 用户消息 + 文件内容摘要                                    │
│    输出: {                                                        │
│      "intent": "expense_create",                                  │
│      "confidence": 0.95,                                          │
│      "parameters": {"amount": 25, "category": "饮食"}            │
│    }                                                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. 任务分解 + 发布                                                │
│    Intent = "expense_create"                                      │
│    → 发布任务到 queue.expense                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Specialist Agent 并行消费                                      │
│    ExpenseAgent:                                                  │
│      - 查询用户口味偏好                                            │
│      - 写入 expense 表                                            │
│      - 返回结果                                                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. 降级策略                                                       │
│    重试 3 次失败后:                                                │
│      - 调用 LLM 预测结果                                           │
│      - 标记为 "fallback" 模式                                     │
│      - 返回给用户说明情况                                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. 结果聚合 + 返回                                                │
│    - 收集各 Agent 结果                                            │
│    - 格式化输出                                                    │
│    - 通过 WebSocket 推送                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 多工具并行场景

```
用户: "帮我记账花了25元，同时记录今天的课程（上传了课表图片）"

分析结果:
  - Task 1: {agent: "expense", action: "create", params: {amount: 25, category: "饮食"}}
  - Task 2: {agent: "course", action: "import", params: {file: "base64..."}}

发布任务:
  → queue.expense: {taskId: "uuid1", ...}
  → queue.course:  {taskId: "uuid2", ...}

并行执行:
  ┌─────────────────┐     ┌─────────────────┐
  │  ExpenseAgent   │     │   CourseAgent   │
  │  - 写入数据库   │     │  - OCR 识别     │
  │  - 返回结果     │     │  - 课程导入     │
  └────────┬────────┘     └────────┬────────┘
           │                       │
           ▼                       ▼
      {success: true,         {success: true,
       expenseId: 123}         coursesImported: 5}

结果聚合:
  "已为您记账：饮食消费 25 元。同时已导入 5 门课程到您的课表。"
```

---

## 5. 消息队列设计

### 5.1 队列结构

| 队列名称 | 消费者 | 消息类型 |
|---------|--------|---------|
| agent.weather | WeatherAgent | TaskMessage |
| agent.expense | ExpenseAgent | TaskMessage |
| agent.course | CourseAgent | TaskMessage |
| agent.profile | ProfileAgent | TaskMessage |
| agent.note | NoteAgent | TaskMessage |
| agent.results | Orchestrator | ResultMessage |

### 5.2 消息格式

```java
// 任务消息
public class TaskMessage {
    String taskId;           // 任务ID
    String agent;            // 目标Agent
    String action;           // 操作类型
    Long userId;             // 用户ID
    Map<String, Object> params;  // 参数
    String correlationId;    // 关联ID（用于结果匹配）
    int retryCount;          // 重试次数
    long timestamp;           // 时间戳
}

// 结果消息
public class ResultMessage {
    String taskId;
    String agent;
    boolean success;
    Object result;          // 执行结果
    String errorMessage;     // 错误信息
    boolean fallback;        // 是否降级
    long executionTimeMs;    // 执行耗时
    String correlationId;
}
```

---

## 6. 工具定义

### 6.1 天气查询
```json
{
  "name": "weather_query",
  "description": "查询指定城市的天气信息",
  "parameters": {
    "type": "object",
    "properties": {
      "city": {"type": "string", "description": "城市名称"},
      "type": {"type": "string", "enum": ["current", "forecast"], "description": "查询类型"}
    },
    "required": ["city"]
  }
}
```

### 6.2 记账创建
```json
{
  "name": "expense_create",
  "description": "创建一条记账记录",
  "parameters": {
    "type": "object",
    "properties": {
      "amount": {"type": "number", "description": "金额"},
      "category": {"type": "string", "description": "消费类别：饮食/交通/购物/娱乐/其他"},
      "description": {"type": "string", "description": "备注"},
      "date": {"type": "string", "description": "消费日期 YYYY-MM-DD"}
    },
    "required": ["amount", "category"]
  }
}
```

### 6.3 课程导入
```json
{
  "name": "course_import",
  "description": "解析文件并导入课程到用户课表",
  "parameters": {
    "type": "object",
    "properties": {
      "fileData": {"type": "string", "description": "文件Base64或URL"},
      "fileType": {"type": "string", "enum": ["image", "pdf", "excel", "document"], "description": "文件类型"},
      "semesterStart": {"type": "string", "description": "学期开始日期 YYYY-MM-DD"}
    },
    "required": ["fileData", "fileType"]
  }
}
```

### 6.4 个人信息修改
```json
{
  "name": "profile_update",
  "description": "修改用户个人信息",
  "parameters": {
    "type": "object",
    "properties": {
      "height": {"type": "number", "description": "身高(cm)"},
      "weight": {"type": "number", "description": "体重(kg)"},
      "nickname": {"type": "string", "description": "昵称"},
      "city": {"type": "string", "description": "城市"},
      "tastePreference": {"type": "string", "description": "口味偏好"}
    }
  }
}
```

### 6.5 笔记生成
```json
{
  "name": "note_generate",
  "description": "根据课程内容生成学习笔记",
  "parameters": {
    "type": "object",
    "properties": {
      "courseName": {"type": "string", "description": "课程名称"},
      "content": {"type": "string", "description": "课程内容或用户输入"},
      "type": {"type": "string", "enum": ["summary", "note", "review"], "description": "笔记类型"}
    },
    "required": ["courseName", "content"]
  }
}
```

---

## 7. 降级策略

### 7.1 重试机制
```
执行失败 → 等待 1s → 重试
       → 等待 2s → 重试
       → 等待 3s → 重试
       → 失败 → 降级
```

### 7.2 降级处理
```java
public class FallbackHandler {
    // 天气降级：返回"天气服务暂时不可用"
    String weatherFallback();

    // 记账降级：返回模拟结果，标记 fallback=true
    Map<String, Object> expenseFallback(params);

    // 课程降级：提取文本，提示用户确认
    Map<String, Object> courseFallback(params);

    // 资料修改降级：返回错误，提示用户手动修改
    Map<String, Object> profileFallback(params);

    // 笔记降级：调用 LLM 直接生成
    String noteFallback(params);
}
```

---

## 8. 数据库变更

### 8.1 新增表

**agent_task_log** (任务执行日志)
```sql
CREATE TABLE agent_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    agent VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    params TEXT,
    result TEXT,
    status VARCHAR(16) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    execution_time_ms BIGINT,
    fallback BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);
```

---

## 9. API 设计

### 9.1 对话接口
```
POST /api/agent/chat
Headers:
  Authorization: Bearer {token}
  Content-Type: multipart/form-data

Body:
  message: "帮我记账花了25元"
  files[]: (可选，文件上传)

Response:
  {
    "success": true,
    "message": "已为您记账：饮食消费 25 元",
    "tasks": [
      {taskId: "uuid1", agent: "expense", status: "completed", result: {...}}
    ]
  }
```

### 9.2 任务状态查询
```
GET /api/agent/tasks/{taskId}

Response:
  {
    "taskId": "uuid1",
    "status": "completed",
    "result": {...},
    "executionTimeMs": 123
  }
```

---

## 10. 目录结构

```
backend/src/main/java/com/qingtu/agent/
├── agent/
│   ├── orchestrator/
│   │   ├── OrchestratorAgent.java          # 编排器主类
│   │   ├── IntentAnalyzer.java             # 意图分析
│   │   ├── TaskDecomposer.java             # 任务分解
│   │   ├── ResultAggregator.java           # 结果聚合
│   │   └── UserContextProvider.java        # 用户上下文提供者
│   ├── specialist/
│   │   ├── WeatherAgent.java               # 天气Agent
│   │   ├── ExpenseAgent.java              # 记账Agent
│   │   ├── CourseAgent.java               # 课程Agent
│   │   ├── ProfileAgent.java              # 资料Agent
│   │   └── NoteAgent.java                 # 笔记Agent
│   ├── message/
│   │   ├── TaskMessage.java                # 任务消息
│   │   ├── ResultMessage.java             # 结果消息
│   │   └── AgentMessagePublisher.java     # 消息发布
│   └── fallback/
│       └── FallbackHandler.java           # 降级处理
```

---

## 11. 实现优先级

| 优先级 | 功能 | 说明 |
|-------|------|------|
| P0 | Orchestrator 基础框架 | 意图分析 + 任务分发 |
| P0 | Weather Agent | 最简单，先验证流程 |
| P0 | Expense Agent | 核心功能，验证数据库写入 |
| P1 | Course Agent | 复杂（文件解析 + 多步骤） |
| P1 | Profile Agent | 修改资料 |
| P2 | Note Agent | 笔记生成 |
| P3 | WebSocket 实时推送 | 提升体验 |

---

## 12. 风险与挑战

1. **文件 Base64 传输**：大文件可能导致请求超时 → 需要分片上传
2. **Agent 并发控制**：RabbitMQ 消费者数量需限制 → 防止数据库连接池耗尽
3. **上下文丢失**：多 Agent 执行时，用户上下文需要正确传递
4. **降级准确性**：LLM 预测可能不准确 → 需要明确告知用户

---

**文档状态**: 待审核
**下一步**: 生成实现计划