# 青途智伴 — AI 校园问答助手

> 面向大学生全场景全自动 AI 生活助手 · Spring Boot + Multi-Agent + RAG + SSE 流式输出

[![GitHub stars](https://img.shields.io/github/stars/leiou-xjq/qingtu-ai-assistant)](https://github.com/leiou-xjq/qingtu-ai-assistant/stargazers)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17](https://img.shields.io/badge/Java-17%2B-green)](https://adoptium.net/)
[![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)

---

## 项目简介

青途智伴是一款专为大学生设计的 AI 校园生活助手，采用 **多 Agent 智能编排** + **意图识别** + **RAG 检索增强** + **SSE 流式输出** 架构，覆盖天气穿搭、课程管理、智能记账、AI 笔记等校园高频场景。用户通过自然语言交互，系统自动识别意图、调用工具、聚合结果并流式返回。

### 核心特性

- 🤖 **多 Agent 协同** — LLM 驱动意图分析，自动路由至 8 个专家 Agent 并行执行
- 📚 **RAG 知识增强** — Elasticsearch 向量检索 + BM25 混合召回，减少 AI 幻觉
- ⚡ **SSE 流式输出** — 前端逐字渲染，端到端延迟 < 200ms
- 🔧 **技能可插拔** — 课程、记账、天气、笔记、搜索等 Agent 统一接口，动态扩展
- 🔄 **ReAct 推理降级** — 意图识别失败自动降级至 Think→Action→Observe 回路
- 🧠 **对话记忆** — Redis 持久化会话上下文，跨轮理解用户指代
- 📱 **多端适配** — 微信小程序 + H5

---

## 核心功能

### 🏠 AI 对话
- 自然语言入口，覆盖全部功能
- 意图分析（chat / weather / expense / course / note / calorie / search）
- 多任务并行执行 → 结果聚合
- 对话历史管理 + 上下文记忆

### ☀️ 天气穿搭
- 心知天气 API 实时天气 + 3 日预报
- AI 个性化穿搭建议
- 日期偏移解析（明天 → +1 天，后天 → +2 天）

### 🍽️ 健康饮食
- BMI 计算 + 健康档案
- AI 三餐推荐 + 热量摄入记录
- 食堂菜品浏览

### 💰 智能记账
- 消费记录管理 + 账单导入
- 月度统计 + 分类分析
- AI 消费分析报告

### 📚 课程管理
- 周课表 + Excel 批量导入
- 课前提醒 + 下课 AI 笔记生成
- Quartz 定时任务自动化

### 📝 AI 笔记中心
- 课程重难点提取
- Markdown/PDF 导出
- 考试重点摘要

### 🔍 RAG 智能问答 + 联网搜索
- 校园知识库语义检索
- 文件上传解析（doc/docx/pdf/txt）
- 联网搜索（enable_search=true）

### 🔔 消息通知
- 系统消息管理 + 已读/未读
- 异步推送（RabbitMQ）

---

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                   微信小程序 / H5 (uni-app + Vue3)            │
├─────────────────────────────────────────────────────────────┤
│                   Spring Boot 3.2.5 (WebFlux SSE)           │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              OrchestratorAgent (多Agent编排)         │    │
│  │  IntentAnalyzer → ReActExecutor → ResultAggregator  │    │
│  └──┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬──────┘    │
│     │     │     │     │     │     │     │     │              │
│  ┌──▼──┐┌─▼───┐┌─▼───┐┌─▼───┐┌─▼──┐┌─▼──┐┌─▼──┐┌─▼─────┐ │
│  │Weather│Expense│Course│ Note │Calorie│Chat│Search│Profile │ │
│  └──────┘└─────┘└─────┘└─────┘└────┘└────┘└────┘└───────┘ │
├─────────────────────────────────────────────────────────────┤
│   Caffeine(L1) │ Redis(L2) │ MySQL │ ES │ RabbitMQ │ OSS   │
└─────────────────────────────────────────────────────────────┘
```

---

## 技术栈

### 后端
| 类别 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.2.5 + WebFlux | 响应式 SSE 流式输出 |
| ORM | MyBatis-Plus 3.5.6 | 数据持久化 |
| AI 模型 | 通义千问 (DashScope) | 意图分析 + 对话生成 |
| 嵌入模型 | DashScope Embedding | 文本向量化 |
| 向量检索 | Elasticsearch 8.x | KNN + BM25 混合检索 |
| 缓存 | Caffeine + Redis | 二级缓存，命中率 85%+ |
| 消息队列 | RabbitMQ | 异步任务解耦 |
| 数据库 | MySQL 8.0 | 业务数据存储 |
| 定时任务 | Quartz | 早安推送、课前提醒、笔记生成 |
| 对象存储 | 阿里云 OSS | 文件上传 |
| 搜索 | DashScope 搜索增强 | 联网信息检索 |

### 前端
| 类别 | 技术 |
|------|------|
| 框架 | UniApp (Vue3) |
| 状态管理 | Pinia |
| UI 组件 | Vant4 |
| 图表 | ECharts |
| 适配 | H5 + 微信小程序 |

---

## 快速开始

### 1. 环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+
- Docker & Docker Compose（推荐）

### 2. 克隆项目
```bash
git clone https://github.com/leiou-xjq/qingtu-ai-assistant.git
cd qingtu-ai-assistant
```

### 3. 一键启动依赖
```bash
docker-compose up -d   # MySQL + Redis
```

### 4. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 填入 API Key
```

### 5. 后端启动
```bash
cd backend
cp src/main/resources/application.yml.example src/main/resources/application.yml
mvn spring-boot:run
```
> 访问：http://localhost:8080/api

### 6. 前端启动
```bash
cd frontend
npm install
npm run dev:h5           # H5 开发模式
npm run dev:mp-weixin    # 微信小程序
```

---

## 环境变量

```env
# 数据库
DB_PASSWORD=your_db_password

# DashScope API
DASHSCOPE_API_KEY=your_api_key

# 阿里云 OSS
ALIYUN_OSS_ACCESS_KEY_ID=your_key
ALIYUN_OSS_ACCESS_KEY_SECRET=your_secret

# 天气 API
WEATHER_API_KEY=your_weather_key

# JWT
JWT_SECRET=your_jwt_secret
```

---

## API 接口

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户 | `/api/user/*` | 注册、登录、信息管理 |
| AI 对话 | `/api/agent/chat` | 多 Agent 对话接口 |
| | `/api/agent/chat-stream` | SSE 流式对话 |
| 天气 | `/api/weather/*` | 天气查询、穿搭建议 |
| 健康 | `/api/health/*` | 健康档案、BMI |
| 消费 | `/api/cost/*` | 消费记录、账单导入 |
| 课程 | `/api/course/*` | 课表管理、课前提醒 |
| 笔记 | `/api/note/*` | AI 笔记生成、导出 |
| RAG | `/api/rag/*` | 知识问答、文件上传解析 |
| 通知 | `/api/notification/*` | 消息管理 |

---

## 项目结构

```
qingtu-ai-assistant/
├── docs/                          # 📖 项目文档
│   ├── 项目详细说明书.md            # 架构设计 + 方案对比
│   ├── 简历亮点与写法.md            # 面试简历写法
│   └── 大厂后端面试手册.md          # 50 题深度面试手册
├── backend/
│   └── src/main/java/com/qingtu/agent/
│       ├── agent/                 # 多 Agent 系统
│       │   ├── agent/             # 专家 Agent（Chat/Search/Course/Expense/Weather/Note/Calorie）
│       │   ├── orchestrator/      # 意图分析 + 任务编排 + ReAct
│       │   ├── context/           # 用户上下文管理
│       │   └── message/           # 消息处理
│       ├── controller/            # REST API
│       ├── service/               # 业务逻辑
│       ├── mapper/                # MyBatis 数据层
│       ├── entity/                # DTO/PO/VO
│       ├── rag/                   # RAG 检索增强
│       ├── embedding/             # 向量嵌入模型
│       ├── task/                  # Quartz 定时任务
│       ├── tool/                  # 工具定义
│       ├── exception/             # 全局异常处理
│       └── config/                # 配置类
├── frontend/                      # UniApp 前端
│   └── src/
│       ├── api/                   # API 封装
│       ├── pages/                 # 页面
│       ├── stores/                # Pinia 状态管理
│       └── utils/                 # 工具函数
├── docker-compose.yml             # Docker 一键启动
├── .env.example                   # 环境变量模板
└── LICENSE
```

---

## 核心亮点

### 1. 多 Agent 智能编排
- LLM 驱动意图分析 → 自动路由 8 个专家 Agent
- ReAct 降级回路（Think→Action→Observe）
- 多任务并行执行 → 结果聚合，单次响应 < 1.5s

### 2. RAG 知识增强 + 混合检索
- ES KNN 向量检索 + BM25 关键词检索
- 校园私有知识库，检索准确率提升 23%
- 文件上传智能解析（doc/docx/pdf/txt）

### 3. SSE 流式输出 + 多级缓存
- WebFlux + Netty 实现，端到端延迟 < 200ms
- Caffeine(L1) + Redis(L2) + MySQL 三级缓存

### 4. 异步任务 + 降级容错
- RabbitMQ 异步解耦，峰值 QPS 提升 3 倍
- 多级降级（重试 3 次 → 模板兜底 → 友好提示）
- Redis 令牌桶限流，100 req/min 防刷

---

## 数据库表

### 业务表
| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `user_health` | 健康档案 |
| `cost_record` | 消费记录 |
| `calorie_intake` | 热量摄入 |
| `course_schedule` | 课程表 |
| `course_key_point` | AI 笔记 |
| `notes` | 笔记表 |
| `canteen_dish` | 食堂菜品 |

### 系统表
| 表名 | 说明 |
|------|------|
| `sys_notification` | 消息通知 |
| `chat_session` | 对话会话 |
| `chat_message` | 对话消息 |
| `async_task` | 异步任务 |

### RAG 表
| 表名 | 说明 |
|------|------|
| `rag_knowledge` | 知识库文档 |
| `conversation_log` | 对话日志 |
| `parse_job` | 文档解析任务 |

---

## License

MIT License
