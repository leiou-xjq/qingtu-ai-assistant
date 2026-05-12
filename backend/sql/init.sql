=========================================
青途智伴AI生活助手 - 数据库初始化脚本
=========================================
执行方式：
  mysql -u root -p < init.sql
=========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS qingtu_assistant DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qingtu_assistant;

-- ==========================================
-- 以下是 MyBatis-Plus 自动建表的补充表
-- 如需全部建表，请确保应用已启动过或手动创建基表
-- ==========================================

-- 记账记录表
CREATE TABLE IF NOT EXISTS `expense` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10, 2) NOT NULL COMMENT '金额',
    `category` VARCHAR(32) DEFAULT '其他' COMMENT '消费类别：饮食/交通/购物/娱乐/其他',
    `description` VARCHAR(255) DEFAULT '' COMMENT '备注',
    `record_date` DATE DEFAULT (CURRENT_DATE) COMMENT '消费日期',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_record_date (`record_date`),
    INDEX idx_category (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记账记录表';

-- 笔记表
CREATE TABLE IF NOT EXISTS `notes` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) NOT NULL COMMENT '笔记标题',
    `content` TEXT COMMENT '笔记内容',
    `note_type` VARCHAR(32) DEFAULT 'summary' COMMENT '笔记类型：summary/note/review',
    `course_id` BIGINT DEFAULT NULL COMMENT '关联课程ID',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_course_id (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习笔记表';

-- Agent任务日志表
CREATE TABLE IF NOT EXISTS `agent_task_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `agent` VARCHAR(32) NOT NULL COMMENT 'Agent类型',
    `action` VARCHAR(32) NOT NULL COMMENT '动作类型',
    `user_id` BIGINT COMMENT '用户ID',
    `params` TEXT COMMENT '任务参数JSON',
    `result` TEXT COMMENT '执行结果JSON',
    `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `execution_time_ms` BIGINT COMMENT '执行耗时毫秒',
    `fallback` TINYINT DEFAULT 0 COMMENT '是否降级：0-否，1-是',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_id (`task_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`),
    INDEX idx_agent (`agent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent任务执行日志表';

-- MCP 审计日志表
CREATE TABLE IF NOT EXISTS `mcp_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `tool_name` VARCHAR(100) DEFAULT NULL COMMENT '工具名称',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '工具分类：school/rag/doc/weather',
    `arguments` TEXT COMMENT '调用参数JSON',
    `result` TEXT COMMENT '返回结果JSON',
    `status` VARCHAR(20) DEFAULT NULL COMMENT '状态：SUCCESS/FAILED',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时ms',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tool_name` (`tool_name`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP工具调用审计日志表';

-- 解析任务表
CREATE TABLE IF NOT EXISTS `parse_job` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件URL',
    `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    `progress` INT DEFAULT 0 COMMENT '进度',
    `result` TEXT COMMENT '解析结果JSON',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `clarifying_questions` TEXT COMMENT '澄清问题JSON',
    `confirmed_data` TEXT COMMENT '确认数据JSON',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `completed_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档解析任务表';

-- 爬取记录表
CREATE TABLE IF NOT EXISTS `rag_crawl_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `school_website` VARCHAR(500) NOT NULL COMMENT '学校官网地址',
    `school_name` VARCHAR(100) COMMENT '学校名称',
    `page_type` VARCHAR(50) COMMENT 'campus|announce|canteen|course|exam',
    `page_url` VARCHAR(500) COMMENT '被抓取的页面URL',
    `status` INT DEFAULT 0 COMMENT '0待抓取,1成功,2失败',
    `page_count` INT DEFAULT 0 COMMENT '抓取页面数',
    `error_msg` VARCHAR(500) COMMENT '错误信息',
    `crawl_time` DATETIME COMMENT '抓取时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_school` (`school_website`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG爬取记录表';

-- AI对话日志表
CREATE TABLE IF NOT EXISTS `conversation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `trace_id` VARCHAR(64) COMMENT '链路追踪ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(64) COMMENT '会话ID',
    `role` ENUM('user', 'assistant', 'system') NOT NULL COMMENT '角色',
    `content` TEXT NOT NULL COMMENT '对话内容',
    `intent` VARCHAR(32) COMMENT '意图分类',
    `tool_used` VARCHAR(64) COMMENT '使用的工具',
    `token_used` INT COMMENT 'Token消耗',
    `latency_ms` INT COMMENT '响应延迟(毫秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_session` (`session_id`),
    INDEX `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话日志表';

-- 异步RAG任务表
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `question` TEXT COMMENT '用户问题',
    `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
    `answer` TEXT COMMENT 'AI回答',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步RAG任务表';

-- ==========================================
-- 以下为 ALTER TABLE 语句（依赖应用已自动建表）
-- 如遇错误可忽略，说明表尚未创建
-- ==========================================

-- RAG爬取相关字段
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `school_website` VARCHAR(500) COMMENT '学校官网地址' AFTER `nickname`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `school_name` VARCHAR(100) COMMENT '学校名称' AFTER `school_website`;
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `last_crawl_time` DATETIME COMMENT '最后抓取时间' AFTER `school_name`;

ALTER TABLE `rag_knowledge` ADD COLUMN IF NOT EXISTS `school_website` VARCHAR(500) COMMENT '学校官网地址' AFTER `category`;
ALTER TABLE `rag_knowledge` ADD COLUMN IF NOT EXISTS `school_name` VARCHAR(100) COMMENT '学校名称' AFTER `school_website`;

-- 通知表跳转字段
ALTER TABLE `sys_notification` ADD COLUMN IF NOT EXISTS `target_page` VARCHAR(128) DEFAULT NULL COMMENT '跳转页面';
ALTER TABLE `sys_notification` ADD COLUMN IF NOT EXISTS `detail_id` VARCHAR(64) DEFAULT NULL COMMENT '详情ID';
ALTER TABLE `sys_notification` ADD COLUMN IF NOT EXISTS `cached_content` TEXT COMMENT '预生成缓存内容';

-- 索引优化
ALTER TABLE `user` ADD INDEX IF NOT EXISTS `idx_city` (`city`);
ALTER TABLE `course_schedule` ADD INDEX IF NOT EXISTS `idx_user_weekday` (`user_id`, `weekday`, `deleted`);
ALTER TABLE `course_schedule` ADD INDEX IF NOT EXISTS `idx_user_weekday_range` (`user_id`, `weekday`, `week_start`, `week_end`, `deleted`);
ALTER TABLE `course_key_point` ADD INDEX IF NOT EXISTS `idx_user_week` (`user_id`, `week_num`);
ALTER TABLE `course_key_point` ADD INDEX IF NOT EXISTS `idx_user_course` (`user_id`, `course_id`);
