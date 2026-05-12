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