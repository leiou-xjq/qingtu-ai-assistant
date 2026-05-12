# 青途智伴AI生活助手

> 面向大学生全场景全自动AI生活助手

[![GitHub stars](https://img.shields.io/github/stars/leiou-xjq/qingtu-ai-assistant)](https://github.com/leiou-xjq/qingtu-ai-assistant/stargazers)
[![CI](https://github.com/leiou-xjq/qingtu-ai-assistant/actions/workflows/ci.yml/badge.svg)](https://github.com/leiou-xjq/qingtu-ai-assistant/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 17](https://img.shields.io/badge/Java-17%2B-green)](https://adoptium.net/)
[![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)

## 项目简介

青途智伴是一款专为大学生设计的AI生活助手，采用 **多智能体协同** + **MCP工具调度** + **RAG检索增强** + **Quartz自动定时任务** 架构，覆盖天气穿搭、课程管理、智能记账、AI笔记等校园生活场景。

### 核心特性

- 🤖 **AI 对话** - 接入通义千问/豆包大模型，支持多轮对话
- 🔄 **MCP 调度** - 统一工具调度中心，标准化工具注册与调用
- 📚 **RAG 知识增强** - 私有知识库语义检索，减少 AI 幻觉
- ⏰ **全自动定时任务** - Quartz 驱动早安推送、课前提醒、笔记生成
- 🔧 **技能可插拔** - 8个独立技能模块，动态启用/禁用
- 📱 **多端适配** - 微信小程序 + H5 双端运行

## 核心功能

### 🏠 首页仪表盘
- 实时天气展示
- 今日课程概览
- 消费统计数据
- AI智能问答入口
- 未读消息提醒

### ☀️ 天气穿搭
- 实时天气查询（心知天气 API）
- AI个性化穿搭建议
- 7日天气预报
- 每日早安自动推送

### 🍽️ 健康饮食
- BMI自动计算
- 健康档案管理
- AI三餐推荐
- 食堂菜品浏览

### 💰 智能记账
- 消费记录管理
- 微信/支付宝账单导入
- 月度消费统计
- AI消费分析报告

### 📚 课程管理
- 周课表展示
- Excel批量导入
- 课前15分钟提醒
- 下课AI笔记生成

### 📝 AI笔记中心
- 课程重难点自动生成
- 考试重点提取
- 每日笔记汇总
- Markdown/PDF导出

### 🤖 RAG智能问答
- 校园知识库检索
- 基于私有知识库回答
- 减少AI幻觉

### 🔔 消息通知
- 系统消息管理
- 已读/未读状态
- 分类筛选

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        微信小程序 / H5                        │
├─────────────────────────────────────────────────────────────┤
│                        UniApp (Vue3)                        │
├─────────────────────────────────────────────────────────────┤
│                      Spring Boot 3.2.5                       │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐         │
│  │  Agent  │ │   MCP   │ │  Skill  │ │   RAG   │         │
│  │  多智能体 │ │ 调度中心  │ │ 技能系统  │ │ 知识检索  │         │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘         │
├─────────────────────────────────────────────────────────────┤
│  Quartz │ Redis │ MySQL │ Elasticsearch │ Chroma          │
└─────────────────────────────────────────────────────────────┘
```

## 技术栈

### 后端
- **框架**: SpringBoot 3.2.5
- **ORM**: MyBatis-Plus 3.5.6
- **AI**: 通义千问 (DashScope) + 豆包 (Doubao)
- **定时任务**: Quartz
- **向量库**: Chroma (内嵌) + Elasticsearch
- **缓存**: Caffeine + Redis
- **消息队列**: RabbitMQ
- **数据库**: MySQL 8.0

### 前端
- **框架**: UniApp (Vue3)
- **状态管理**: Pinia
- **UI组件**: Vant4
- **图表**: ECharts
- **适配**: H5 + 微信小程序 + APP

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

### 3. 一键启动依赖（Docker Compose）
```bash
# 启动 MySQL + Redis（自动初始化数据库）
docker-compose up -d
```

### 4. 配置环境变量
```bash
# 复制环境变量模板，填入你的 API Key
cp .env.example .env
# 编辑 .env 填入实际配置
```

### 5. 后端启动
```bash
cd backend

# 复制配置文件
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 启动应用
mvn spring-boot:run
```
> 应用启动后访问：http://localhost:8080/api

### 6. 前端启动
```bash
cd frontend
npm install

# H5开发模式
npm run dev:h5

# 微信小程序 (需安装微信开发者工具)
npm run dev:mp-weixin
```

### 7. 手动数据库初始化（不使用 Docker 时）
```bash
# 创建数据库并执行初始化脚本
mysql -u root -p < backend/sql/init.sql
```

## 配置说明

### 1. 配置文件模板
项目使用 `application.yml.example` 作为配置模板，敏感信息通过环境变量管理。

```bash
# 复制配置文件模板
cp backend/src/main/resources/application.yml.example \
   backend/src/main/resources/application.yml

# 复制环境变量模板
cp .env.example .env  # 如有 .env.example
```

### 2. 环境变量 (.env)
复制 `.env.example` 为 `.env`，并填入实际配置：

```bash
cp .env.example .env
# 编辑 .env 填入你的 API Key
```

```env
# 数据库
DB_PASSWORD=your_db_password

# Spring AI (阿里云百炼)
SPRING_AI_API_KEY=your_dashscope_api_key
SPRING_AI_MODEL=qwen-turbo

# 阿里云 OSS
ALIYUN_OSS_ACCESS_KEY_ID=your_access_key
ALIYUN_OSS_ACCESS_KEY_SECRET=your_secret_key

# 天气 API
WEATHER_API_KEY=your_weather_api_key

# 地图 API
BAIDU_MAP_AK=your_baidu_map_ak
QQ_MAP_KEY=your_qq_map_key

# JWT
JWT_SECRET=your_jwt_secret

# UniPush
UNIPUSH_APPKEY=your_unipush_appkey
UNIPUSH_APPSECRET=your_unipush_appsecret
```

> **注意**: `.env` 文件包含敏感信息，已加入 `.gitignore`，不会提交到版本库。

### 3. application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qingtu_assistant
    username: root
    password: ${DB_PASSWORD}  # 从环境变量读取
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

# AI配置 (使用环境变量)
ai:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
```

### 4. 环境变量 (Linux/Mac)
```bash
export DB_PASSWORD=your_db_password
export DASHSCOPE_API_KEY=your_api_key
export WEATHER_API_KEY=your_weather_key
```

### 5. 环境变量 (Windows PowerShell)
```powershell
$env:DB_PASSWORD="your_db_password"
$env:DASHSCOPE_API_KEY="your_api_key"
$env:WEATHER_API_KEY="your_weather_key"
```

## 项目结构

```
qingtu-ai-assistant/
├── .gitignore               # Git忽略配置
├── .env.example             # 环境变量模板
├── docker-compose.yml       # Docker 一键启动
├── LICENSE                  # MIT 许可证
├── README.md
│
├── .github/workflows/       # GitHub Actions CI
│   └── ci.yml
│
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/com/qingtu/agent/
│   │   ├── agent/           # AI Agent 模块
│   │   │   ├── agent/       # 专家智能体 (Weather, Course, Chat...)
│   │   │   ├── context/     # 用户上下文
│   │   │   ├── message/     # 消息处理
│   │   │   ├── orchestrator/ # 意图分析与任务编排
│   │   │   └── fallback/    # 降级处理
│   │   ├── mcp/             # MCP 调度中心
│   │   │   └── server/      # MCP Server 实现
│   │   ├── config/          # 配置类
│   │   ├── controller/      # REST API 接口
│   │   ├── service/         # 业务逻辑层
│   │   │   └── impl/        # 业务实现
│   │   ├── entity/          # 实体类 (dto/po/vo)
│   │   ├── mapper/          # MyBatis 数据访问层
│   │   ├── rag/             # RAG 检索增强生成
│   │   ├── task/            # Quartz 定时任务
│   │   ├── tool/            # 工具 (Weather, WebSearch...)
│   │   ├── util/            # 工具类
│   │   └── exception/       # 异常处理
│   ├── src/main/resources/
│   │   ├── application.yml.example  # 配置模板
│   │   └── schools.json     # 学校数据
│   ├── sql/
│   │   └── init.sql         # 数据库初始化脚本
│   └── pom.xml
│
├── frontend/                # UniApp 微信小程序
│   ├── src/
│   │   ├── api/             # API 封装
│   │   ├── pages/           # 页面
│   │   ├── stores/          # Pinia 状态管理
│   │   ├── styles/          # 样式
│   │   └── utils/           # 工具函数
│   ├── pages.json           # 页面路由配置
│   ├── package.json
│   └── vite.config.js
│
└── web/                     # Web H5 版本
    ├── src/
    ├── index.html
    ├── package.json
    └── vite.config.js
```

## API接口

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户 | /user/* | 注册、登录、信息管理 |
| 天气 | /weather/* | 天气查询、穿搭建议 |
| 健康 | /health/* | 健康档案、BMI计算 |
| 菜品 | /dish/* | 菜品浏览、AI推荐 |
| 消费 | /cost/* | 消费记录、账单导入 |
| 课程 | /course/* | 课表管理、提醒设置 |
| 笔记 | /note/* | AI笔记生成、导出 |
| RAG | /rag/* | 智能问答、知识检索 |
| 通知 | /notification/* | 消息管理 |
| 任务 | /task/* | 定时任务配置 |

## 核心亮点

### 1. 多智能体协同
- 基于 MCP 服务调度实现意图分析 + 专家智能体 + 任务编排
- 8个独立技能模块，统一接口设计，支持动态启用/禁用

### 2. RAG 知识增强
- Elasticsearch + Chroma 向量数据库，语义相似度检索
- 校园私有知识库，有效减少 AI 幻觉

### 3. 全自动 Quartz 定时任务
- 每日早安天气推送、课前15分钟提醒
- 下课 AI 笔记自动生成、月度消费报告汇总

### 4. Redis 分布式锁
- 基于 setIfAbsent 实现分布式锁，解决集群定时任务重复执行
- Caffeine 本地缓存 + Redis 分布式缓存，加速热点数据访问

## 数据库表

### 核心业务表
| 表名 | 说明 |
|------|------|
| `user` | 用户表 |
| `user_health` | 健康档案表 |
| `canteen_dish` | 食堂菜品表 |
| `cost_record` | 消费记录表 |
| `calorie_intake` | 每日摄入热量表 |
| `course_schedule` | 课程表 |
| `course_key_point` | AI笔记表 |
| `notes` | 笔记表 |

### 系统表
| 表名 | 说明 |
|------|------|
| `sys_notification` | 消息通知表 |
| `sys_task_config` | 定时任务配置表 |
| `chat_session` | AI聊天会话表 |
| `chat_message` | AI聊天消息表 |

### RAG 相关表
| 表名 | 说明 |
|------|------|
| `rag_knowledge` | 知识库文档表 |
| `conversation_log` | 对话日志表 |
| `parse_job` | 文档解析任务表 |

### MCP 相关表
| 表名 | 说明 |
|------|------|
| `mcp_audit_log` | MCP调用审计日志表 |

### 异步任务表
| 表名 | 说明 |
|------|------|
| `async_task` | 异步任务表 |

## License

MIT License