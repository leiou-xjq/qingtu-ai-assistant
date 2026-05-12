-- 青途智伴数据库初始化脚本
-- 请在MySQL中执行

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS qingtu_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE qingtu_ai;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `school` VARCHAR(100) DEFAULT NULL COMMENT '学校',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `role` VARCHAR(20) DEFAULT 'student' COMMENT '角色',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 健康信息表
CREATE TABLE IF NOT EXISTS `user_health` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `height` DECIMAL(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `bmi` DECIMAL(5,2) DEFAULT NULL COMMENT 'BMI指数',
  `activity_level` DECIMAL(3,2) DEFAULT 1.2 COMMENT '活动系数',
  `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户健康信息表';

-- 插入测试用户 (密码: 123456 的MD5)
INSERT INTO `user` (`username`, `password`, `school`, `city`, `role`) 
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '清华大学', '北京', 'student')
ON DUPLICATE KEY UPDATE `username` = 'admin';