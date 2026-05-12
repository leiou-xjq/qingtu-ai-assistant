# 青途智伴AI生活助手

> 面向大学生全场景全自动AI生活助手

[![GitHub stars](https://img.shields.io/github/stars/leiou-xjq/qingtu-ai-assistant)](https://github.com/leiou-xjq/qingtu-ai-assistant/stargazers)
[![GitHub license](https://img.shields.io/github/license/leiou-xjq/qingtu-ai-assistant)](https://github.com/leiou-xjq/qingtu-ai-assistant/blob/main/LICENSE)
[![Java Version](https://img.shields.io/badge/Java-17%2B-green)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)

## 项目简介

青途智伴是一款专为大学生设计的AI生活助手，集成 **AI Agent**、**MCP多组件协同**、**Skill可插拔技能**、**RAG检索增强**、**向量数据库**、**Quartz全自动定时任务**，全流程无人干预自动运行。

### 核心特性

- 🤖 **多智能体协同** - 意图分析 + 专家智能体 + 任务编排
- 🔄 **MCP 协议** - 统一调度中心，标准化工具调用
- 📚 **RAG 知识增强** - 私有知识库检索，减少 AI 幻觉
- ⏰ **全自动定时任务** - 早安推送、课前提醒、笔记生成、报告汇总
- 🔧 **技能可插拔** - 8个独立技能模块，动态启用/禁用
- 📱 **多端适配** - 微信小程序 + H5 + APP

## 核心功能

### 🏠 首页仪表盘
- 实时天气展示
- 今日课程概览
- 消费统计数据
- AI智能问答入口
- 未读消息提醒

### ☀️ 天气穿搭
- 实时天气查询（和风天气API）
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
- **AI**: LangChain4j + 通义千问 (DashScope)
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
- MySQL 8.0+
- Redis
- Maven 3.8+

### 2. 克隆项目
```bash
git clone https://github.com/leiou-xjq/qingtu-ai-assistant.git
cd qingtu-ai-assistant
```

### 3. 配置环境变量
```bash
# 在项目根目录创建 .env 文件
cp .env.example .env  # 或手动创建

# 编辑 .env 文件，填入你的配置
notepad .env
```

### 4. 初始化数据库
```sql
-- 登录 MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE qingtu_assistant DEFAULT CHARSET utf8mb4;

-- 退出后执行 SQL 脚本 (backend/sql/ 目录下)
mysql -u root -p qingtu_assistant < backend/sql/agent_tables.sql
mysql -u root -p qingtu_assistant < backend/sql/conversation_log.sql
mysql -u root -p qingtu_assistant < backend/sql/mcp_audit_log.sql
mysql -u root -p qingtu_assistant < backend/sql/rag_crawler.sql
mysql -u root -p qingtu_assistant < backend/sql/parse_job.sql
mysql -u root -p qingtu_assistant < backend/sql/optimize_index.sql
-- 如有其他 SQL 文件，按需执行
```

### 5. 后端启动
```bash
cd backend

# 使用 Maven 构建
mvn clean package -DskipTests

# 启动应用 (确保已配置环境变量)
java -jar target/qingtu-ai-assistant-1.0.0.jar

# 或开发模式运行
mvn spring-boot:run
```

### 6. 前端启动
```bash
cd frontend

# 安装依赖
npm install

# H5开发模式
npm run dev:h5

# 微信小程序 (需安装微信开发者工具)
npm run dev:mp-weixin
```

### 7. Web H5 启动
```bash
cd web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
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
在项目根目录创建 `.env` 文件，配置以下内容：

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
├── .gitignore              # Git忽略配置
├── .env                    # 本地环境变量 (不提交)
├── README.md
│
├── backend/                # Spring Boot 后端
│   ├── src/main/java/com/qingtu/agent/
│   │   ├── agent/          # AI Agent 模块
│   │   │   ├── agent/      # 专家智能体 (Weather, Course, Chat...)
│   │   │   ├── context/    # 用户上下文
│   │   │   ├── message/    # 消息处理
│   │   │   ├── orchestrator/ # 意图分析与任务编排
│   │   │   └── fallback/   # 降级处理
│   │   ├── mcp/            # MCP 调度中心
│   │   │   └── server/     # MCP Server 实现
│   │   ├── config/         # 配置类
│   │   ├── controller/     # REST API 接口
│   │   ├── service/        # 业务逻辑层
│   │   │   └── impl/       # 业务实现
│   │   ├── entity/         # 实体类
│   │   │   ├── dto/        # 数据传输对象
│   │   │   ├── po/         # 持久化对象
│   │   │   └── vo/         # 视图对象
│   │   ├── mapper/         # MyBatis 数据访问层
│   │   ├── rag/            # RAG 检索增强生成
│   │   ├── task/           # Quartz 定时任务
│   │   ├── tool/           # 工具类 (Weather, WebSearch...)
│   │   ├── util/           # 工具类
│   │   └── exception/      # 异常处理
│   ├── src/main/resources/
│   │   ├── application.yml.example  # 配置模板
│   │   └── schools.json    # 学校数据
│   └── pom.xml
│
├── frontend/               # UniApp 微信小程序
│   ├── src/
│   │   ├── api/            # API 封装
│   │   ├── pages/          # 页面
│   │   │   ├── auth/       # 认证页面
│   │   │   ├── chat/       # AI 聊天
│   │   │   ├── cost/       # 记账
│   │   │   ├── course/     # 课程
│   │   │   ├── diet/       # 饮食
│   │   │   ├── index/      # 首页
│   │   │   ├── note/       # 笔记
│   │   │   ├── notification/ # 通知
│   │   │   ├── profile/    # 个人中心
│   │   │   └── weather/    # 天气
│   │   ├── stores/         # Pinia 状态管理
│   │   ├── styles/         # 样式
│   │   └── utils/          # 工具函数
│   ├── pages.json          # 页面路由配置
│   ├── manifest.json        # 应用配置
│   ├── package.json
│   └── vite.config.js
│
├── web/                    # Web H5 版本
│   ├── src/
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
└── docs/                   # 文档目录
    ├── DEPLOY.md           # 部署文档
    └── 技术文档.md
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

### 1. MCP多组件协同调度
- 统一调度定时任务、Agent、RAG、Skill
- 任务编排与依赖管理
- 异常兜底与降级处理

### 2. Skill可插拔技能体系
- 8个独立技能模块
- 统一接口设计
- 支持动态启用/禁用

### 3. RAG+向量数据库
- 校园私有知识库
- 语义相似度检索
- 减少AI幻觉

### 4. 全自动Quartz定时任务
- 早安天气推送
- 课前提醒
- 下课笔记生成
- 月度消费报告

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