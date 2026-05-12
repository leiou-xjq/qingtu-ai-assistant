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
