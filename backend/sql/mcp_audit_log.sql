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
