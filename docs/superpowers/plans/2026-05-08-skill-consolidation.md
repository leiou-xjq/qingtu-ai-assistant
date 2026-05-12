# Skill系统重构与重复代码整合计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 整合Skill系统中重复的功能组件，消除冗余代码，统一架构

**Architecture:**
- 以SkillFactory为核心Skill注册中心
- 以SkillRouter为唯一意图识别决策器
- 保留MCPCenter用于定时任务调度（委托SkillFactory获取Skill）
- 废弃RAGSkill，统一由CommonRagSkill/SchoolRagSkill处理

**Tech Stack:** Spring Boot 3, Spring AI, Java 17

---

## 背景分析

当前存在以下重复/冗余：

| 组件 | 位置 | 问题 |
|------|------|------|
| IntentClassifier | domain.tool.IntentClassifier | 与SkillRouter功能重叠 |
| MCPCenter.skillRegistry | mcp.MCPCenter:24 | 与SkillFactory.skillRegistry重复 |
| RAGSkill | skill.RAGSkill | 与CommonRagSkill/SchoolRagSkill功能重复 |
| RagServiceCore方法 | rag.RagServiceCore | 4个检索方法功能相似 |
| Prompt构建 | 各Skill实现 | 重复代码 |

---

## 任务分解

### Task 1: 整合IntentClassifier到SkillRouter

**Files:**
- Modify: `backend/src/main/java/com/qingtu/agent/domain/tool/IntentClassifier.java`
- Modify: `backend/src/main/java/com/qingtu/agent/application/service/AIConversationService.java`
- Test: 启动服务测试对话功能

- [ ] **Step 1: 修改IntentClassifier，委托给SkillRouter**
- [ ] **Step 2: 从AIConversationService移除冗余的Intent识别**
- [ ] **Step 3: 编译测试**
- [ ] **Step 4: 提交**

---

### Task 2: 整合MCPCenter的Skill注册到SkillFactory

**Files:**
- Modify: `backend/src/main/java/com/qingtu/agent/mcp/MCPCenter.java`
- Modify: `backend/src/main/java/com/qingtu/agent/task/MorningPushJob.java`
- Modify: `backend/src/main/java/com/qingtu/agent/task/DailySummaryJob.java`
- Modify: `backend/src/main/java/com/qingtu/agent/task/MonthlyReportJob.java`
- Modify: `backend/src/main/java/com/qingtu/agent/task/CourseNoteJob.java`
- Test: 启动定时任务测试

- [ ] **Step 1: 修改MCPCenter，委托SkillFactory获取Skill**
- [ ] **Step 2: 添加SkillFactory到定时任务**
- [ ] **Step 3: 同步修改其他3个定时任务**
- [ ] **Step 4: 编译测试**
- [ ] **Step 5: 提交**

---

### Task 3: 废弃RAGSkill，统一到新RAG Skill

**Files:**
- Modify: `backend/src/main/java/com/qingtu/agent/common/Constants.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/qingtu/agent/skill/SkillFactory.java`

- [ ] **Step 1: 在Constants中添加废弃标记**
- [ ] **Step 2: 配置RAG重定向**
- [ ] **Step 3: 修改SkillFactory支持旧Key映射**
- [ ] **Step 4: 编译测试**
- [ ] **Step 5: 提交**

---

### Task 4: 抽取公共PromptBuilder工具类

**Files:**
- Create: `backend/src/main/java/com/qingtu/agent/skill/PromptBuilder.java`
- Modify: `backend/src/main/java/com/qingtu/agent/skill/CommonRagSkill.java`
- Modify: `backend/src/main/java/com/qingtu/agent/skill/SchoolRagSkill.java`
- Test: 测试各Skill输出

- [ ] **Step 1: 创建PromptBuilder工具类**
- [ ] **Step 2: 重构CommonRagSkill使用PromptBuilder**
- [ ] **Step 3: 同步修改SchoolRagSkill**
- [ ] **Step 4: 编译测试**
- [ ] **Step 5: 提交**

---

### Task 5: 简化RagServiceCore方法

**Files:**
- Modify: `backend/src/main/java/com/qingtu/agent/rag/RagServiceCore.java`
- Test: 测试各检索方法输出

- [ ] **Step 1: 统一入口方法**
- [ ] **Step 2: 标记旧方法为废弃**
- [ ] **Step 3: 编译测试**
- [ ] **Step 4: 提交**

---

## 执行顺序

1. **Task 1** (高优先级) - 消除IntentClassifier与SkillRouter重复
2. **Task 2** (高优先级) - 统一Skill注册中心
3. **Task 3** (中优先级) - 处理RAG Skill遗留
4. **Task 4** (低优先级) - 减少重复代码
5. **Task 5** (低优先级) - 简化接口