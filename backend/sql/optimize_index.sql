-- =========================================
-- 青途智伴AI生活助手 - 数据库索引优化脚本
-- 执行时间：上线前
-- =========================================

-- user 表添加 city 索引
ALTER TABLE `user` ADD INDEX `idx_city` (`city`);

-- course_schedule 表添加复合索引
ALTER TABLE `course_schedule` ADD INDEX `idx_user_weekday` (`user_id`, `weekday`, `deleted`);
ALTER TABLE `course_schedule` ADD INDEX `idx_user_weekday_range` (`user_id`, `weekday`, `week_start`, `week_end`, `deleted`);

-- course_key_point 表添加索引（如果不存在）
ALTER TABLE `course_key_point` ADD INDEX `idx_user_week` (`user_id`, `week_num`);
ALTER TABLE `course_key_point` ADD INDEX `idx_user_course` (`user_id`, `course_id`);

-- =========================================
-- 可选优化：表分析（收集统计信息）
-- =========================================
ANALYZE TABLE `user`;
ANALYZE TABLE `course_schedule`;
ANALYZE TABLE `course_key_point`;

-- =========================================
-- 验证索引是否创建成功
-- =========================================
SHOW INDEX FROM `user`;
SHOW INDEX FROM `course_schedule`;
SHOW INDEX FROM `course_key_point`;