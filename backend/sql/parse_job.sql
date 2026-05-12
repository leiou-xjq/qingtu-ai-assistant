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
