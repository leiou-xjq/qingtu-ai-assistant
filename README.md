# 青途智伴AI生活助手

> 面向大学生全场景全自动AI生活助手

## 项目简介

青途智伴是一款专为大学生设计的AI生活助手，集成AI Agent、MCP多组件协同、Skill可插拔技能、RAG检索增强、向量数据库、Quartz全自动定时任务，全流程无人干预自动运行。

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

## 技术栈

### 后端
- **框架**: SpringBoot 3.2.5
- **ORM**: MyBatis-Plus 3.5.6
- **AI**: LangChain4j + 通义千问
- **定时任务**: Quartz
- **向量库**: Chroma (内嵌)
- **缓存**: Caffeine + Redis
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

### 2. 后端启动
```bash
cd backend

# 修改配置文件
# 编辑 src/main/resources/application.yml
# 配置数据库、Redis、API Key

# 构建项目
mvn clean package -DskipTests

# 启动应用
java -jar target/qingtu-ai-assistant-1.0.0.jar
```

### 3. 前端启动
```bash
cd frontend

# 安装依赖
npm install

# H5开发模式
npm run dev:h5

# 微信小程序
npm run dev:mp-weixin
```

### 4. 导入数据库
```sql
-- 创建数据库
CREATE DATABASE qingtu_assistant DEFAULT CHARSET utf8mb4;

-- 执行SQL脚本
mysql -u root -p qingtu_assistant < src/main/resources/schema.sql
```

## 配置说明

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qingtu_assistant
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

# AI配置
ai:
  dashscope:
    api-key: your_dashscope_api_key

# 天气配置  
weather:
  api-key: your_hefeng_api_key
```

### 环境变量
```bash
export DB_PASSWORD=your_db_password
export DASHSCOPE_API_KEY=your_api_key
export HEFENG_API_KEY=your_weather_key
```

## 项目结构

```
qingtu-ai-assistant/
├── backend/
│   ├── src/main/java/com/qingtu/agent/
│   │   ├── config/          # 配置类
│   │   ├── controller/     # REST接口
│   │   ├── service/        # 业务层
│   │   ├── entity/         # 实体类
│   │   ├── mapper/         # 数据访问层
│   │   ├── agent/          # AI Agent
│   │   ├── mcp/            # MCP调度中心
│   │   ├── skill/          # Skill技能
│   │   ├── rag/            # RAG服务
│   │   ├── task/           # Quartz任务
│   │   ├── util/           # 工具类
│   │   └── exception/      # 异常处理
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
│
├── frontend/
│   ├── src/
│   │   ├── api/            # API封装
│   │   ├── components/     # 公共组件
│   │   ├── pages/         # 页面
│   │   ├── stores/         # Pinia状态
│   │   └── utils/         # 工具
│   └── pages.json
│
└── docs/
    └── README.md
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

- `user` - 用户表
- `user_health` - 健康档案表
- `canteen_dish` - 食堂菜品表
- `cost_record` - 消费记录表
- `course_schedule` - 课程表
- `course_key_point` - AI笔记表
- `sys_task_config` - 定时任务配置表
- `rag_knowledge` - 知识库文档表
- `user_skill_config` - 用户技能配置表
- `sys_notification` - 消息通知表

## License

MIT License