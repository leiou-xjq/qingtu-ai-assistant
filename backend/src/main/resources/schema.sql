-- =============================================
-- 青途智伴AI生活助手 - MySQL数据库建表脚本
-- 数据库：qingtu_assistant
-- MySQL版本：8.0+
-- 字符集：utf8mb4
-- 引擎：InnoDB
-- =============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS qingtu_assistant 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE qingtu_assistant;

-- =============================================
-- 表1：用户表（user）
-- =============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `school` VARCHAR(100) DEFAULT NULL COMMENT '学校名称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `city` VARCHAR(50) DEFAULT NULL COMMENT '所在城市（用于天气查询）',
    `semester_start` DATE DEFAULT NULL COMMENT '学期开始日期',
    `total_weeks` INT DEFAULT 16 COMMENT '教学总周数',
    `dingtalk_webhook` VARCHAR(500) DEFAULT NULL COMMENT '钉钉webhook地址',
    `workweixin_webhook` VARCHAR(500) DEFAULT NULL COMMENT '企微webhook地址',
    `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色（user/admin）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0禁用，1正常）',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 表2：用户健康档案表（user_health）
-- =============================================
DROP TABLE IF EXISTS `user_health`;
CREATE TABLE `user_health` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '健康档案ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `height` DECIMAL(5,1) DEFAULT NULL COMMENT '身高（厘米）',
    `weight` DECIMAL(5,1) DEFAULT NULL COMMENT '体重（公斤）',
    `bmi` DECIMAL(4,2) DEFAULT NULL COMMENT 'BMI值',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `gender` CHAR(1) DEFAULT NULL COMMENT '性别（M男，F女）',
    `activity_level` DECIMAL(2,1) DEFAULT 1.2 COMMENT '活动水平（1.2久坐/1.375轻量/1.55中等/1.75活跃）',
    `diet_goal` VARCHAR(20) DEFAULT 'balance' COMMENT '饮食目标（lose减脂/gain增肌/balance均衡）',
    `daily_calories` INT DEFAULT 1500 COMMENT '每日建议摄入卡路里',
    `taboo_food` VARCHAR(500) DEFAULT NULL COMMENT '饮食忌口（逗号分隔）',
    `taste_preference` VARCHAR(500) DEFAULT NULL COMMENT '口味偏好（逗号分隔）',
    `health_suggestion` TEXT DEFAULT NULL COMMENT '健康建议',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户健康档案表';

-- =============================================
-- 表3：食堂菜品表（canteen_dish）
-- =============================================
DROP TABLE IF EXISTS `canteen_dish`;
CREATE TABLE `canteen_dish` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    `name` VARCHAR(100) NOT NULL COMMENT '菜品名称',
    `type` VARCHAR(20) NOT NULL COMMENT '菜品类型（breakfast早餐/lunch午餐/dinner晚餐/snack夜宵）',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '菜品种类（主食/肉类/蔬菜/汤类/水果等）',
    `calories` INT DEFAULT 0 COMMENT '热量（千卡）',
    `protein` DECIMAL(5,1) DEFAULT 0 COMMENT '蛋白质（克）',
    `fat` DECIMAL(5,1) DEFAULT 0 COMMENT '脂肪（克）',
    `carbs` DECIMAL(5,1) DEFAULT 0 COMMENT '碳水化合物（克）',
    `price` DECIMAL(8,2) DEFAULT 0 COMMENT '价格（元）',
    `canteen` VARCHAR(50) DEFAULT NULL COMMENT '所属食堂',
    `window` VARCHAR(50) DEFAULT NULL COMMENT '窗口位置',
    `tags` VARCHAR(200) DEFAULT NULL COMMENT '标签（减脂/增肌/素食/辣等）',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '菜品图片URL',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '菜品描述',
    `nutrition_info` TEXT DEFAULT NULL COMMENT '营养成分详情（JSON格式）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（0下架，1在售）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    FULLTEXT KEY `ft_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食堂菜品表';

-- =============================================
-- 表4：消费记录表（cost_record）
-- =============================================
DROP TABLE IF EXISTS `cost_record`;
CREATE TABLE `cost_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消费记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '消费金额（元）',
    `category` VARCHAR(20) NOT NULL COMMENT '消费分类（food饮食/transport交通/entertainment娱乐/shopping购物/life生活/study学习/other其他）',
    `source` VARCHAR(20) DEFAULT 'manual' COMMENT '来源（wechat微信/alipay支付宝/manual手动）',
    `trade_no` VARCHAR(100) DEFAULT NULL COMMENT '交易单号',
    `merchant_name` VARCHAR(100) DEFAULT NULL COMMENT '商户名称',
    `trade_time` DATETIME DEFAULT NULL COMMENT '交易时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `tags` VARCHAR(200) DEFAULT NULL COMMENT '标签',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_trade_time` (`trade_time`),
    KEY `idx_source` (`source`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费记录表';

-- =============================================
-- 表5：课程表（course_schedule）
-- =============================================
DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `teacher` VARCHAR(50) DEFAULT NULL COMMENT '授课教师',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '上课地点',
    `weekday` TINYINT NOT NULL COMMENT '星期几（1=周一，7=周日）',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `week_start` INT DEFAULT 1 COMMENT '起始周',
    `week_end` INT DEFAULT 16 COMMENT '结束周',
    `course_type` VARCHAR(20) DEFAULT 'required' COMMENT '课程类型（required必修/elective选修/public公选）',
    `color` VARCHAR(20) DEFAULT '#3B82F6' COMMENT '日历颜色',
    `reminder_enabled` TINYINT DEFAULT 1 COMMENT '是否开启提醒（0否，1是）',
    `reminder_minutes` INT DEFAULT 15 COMMENT '提前提醒分钟数',
    `semester` VARCHAR(20) DEFAULT NULL COMMENT '学期（如：2024春）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_weekday` (`weekday`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_semester` (`semester`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- =============================================
-- 表6：课程AI笔记表（course_key_point）
-- =============================================
DROP TABLE IF EXISTS `course_key_point`;
CREATE TABLE `course_key_point` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `course_name` VARCHAR(100) DEFAULT NULL COMMENT '课程名称',
    `week_num` INT NOT NULL COMMENT '教学周数',
    `class_date` DATE NOT NULL COMMENT '课程日期',
    `class_time` VARCHAR(20) DEFAULT NULL COMMENT '课程时间段',
    `core_points` TEXT DEFAULT NULL COMMENT '核心知识点（JSON格式）',
    `exam_points` TEXT DEFAULT NULL COMMENT '考试重点（JSON格式）',
    `difficult_points` TEXT DEFAULT NULL COMMENT '难点（JSON格式）',
    `易错点` TEXT DEFAULT NULL COMMENT '易错点（JSON格式）',
    `review_guide` TEXT DEFAULT NULL COMMENT '复习指南',
    `summary` TEXT DEFAULT NULL COMMENT '课程总结',
    `ai_model` VARCHAR(50) DEFAULT NULL COMMENT '生成AI模型',
    `tokens_used` INT DEFAULT 0 COMMENT '使用Token数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_week_num` (`week_num`),
    KEY `idx_class_date` (`class_date`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程AI笔记表';

-- =============================================
-- 表7：定时任务配置表（sys_task_config）
-- =============================================
DROP TABLE IF EXISTS `sys_task_config`;
CREATE TABLE `sys_task_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务配置ID',
    `task_key` VARCHAR(50) NOT NULL COMMENT '任务标识（唯一）',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `task_group` VARCHAR(50) DEFAULT 'DEFAULT' COMMENT '任务分组',
    `cron_expression` VARCHAR(100) DEFAULT NULL COMMENT 'Cron表达式',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用（0禁用，1启用）',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '任务描述',
    `params` TEXT DEFAULT NULL COMMENT '任务参数（JSON格式）',
    `last_run_time` DATETIME DEFAULT NULL COMMENT '上次执行时间',
    `next_run_time` DATETIME DEFAULT NULL COMMENT '下次执行时间',
    `run_count` INT DEFAULT 0 COMMENT '执行次数',
    `success_count` INT DEFAULT 0 COMMENT '成功次数',
    `fail_count` INT DEFAULT 0 COMMENT '失败次数',
    `avg_duration` INT DEFAULT 0 COMMENT '平均执行时长（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_key` (`task_key`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务配置表';

-- =============================================
-- 表8：用户技能配置表（user_skill_config）
-- =============================================
DROP TABLE IF EXISTS `user_skill_config`;
CREATE TABLE `user_skill_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '技能配置ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `skill_key` VARCHAR(50) NOT NULL COMMENT '技能标识（weather/outfit/diet/course/cost/note/notification/rag）',
    `skill_name` VARCHAR(100) DEFAULT NULL COMMENT '技能名称',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用（0禁用，1启用）',
    `config_json` TEXT DEFAULT NULL COMMENT '技能配置（JSON格式）',
    `last_used_time` DATETIME DEFAULT NULL COMMENT '最后使用时间',
    `use_count` INT DEFAULT 0 COMMENT '使用次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0未删除，1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_skill` (`user_id`, `skill_key`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户技能配置表';

-- =============================================
-- 初始化定时任务配置数据
-- =============================================
INSERT INTO `sys_task_config` (`task_key`, `task_name`, `task_group`, `cron_expression`, `enabled`, `description`, `params`) VALUES
('morningPush', '每日早安天气穿搭推送', 'PUSH', '0 30 7 * * ?', 1, '每日7:30推送天气和AI穿搭建议', '{"pushHour":7,"pushMinute":30}'),
('courseReminder', '课前提醒', 'REMINDER', '0 45 * * * ?', 1, '课前15分钟自动提醒', '{"reminderMinutes":15}'),
('courseNote', '下课课程笔记生成', 'AI', '0 5,50 * * * ?', 1, '下课后5分钟自动生成课程重难点笔记', '{"generateDelay":5}'),
('dailySummary', '每日笔记汇总', 'PUSH', '0 0 18 * * ?', 1, '每日18:00汇总推送当日所有课程笔记', '{"summaryHour":18}'),
('monthlyReport', '月度消费分析报告', 'AI', '0 0 0 28 * ?', 1, '每月28日生成上月消费分析报告', '{"reportDay":28}'),
('healthReminder', '健康提醒', 'REMINDER', '0 0 12 * * ?', 1, '每日12点健康饮食提醒', '{"reminderHour":12}');

-- =============================================
-- 初始化食堂菜品数据
-- =============================================
INSERT INTO `canteen_dish` (`name`, `type`, `category`, `calories`, `protein`, `fat`, `carbs`, `price`, `canteen`, `window`, `tags`, `description`, `status`) VALUES
-- 早餐
('紫薯粥', 'breakfast', '主食', 120, 2, 1, 25, 2.00, '一食堂', '一楼01号', '清淡,健康', '早上来一碗热腾腾的紫薯粥，养胃又暖心', 1),
('鲜肉包子', 'breakfast', '主食', 180, 8, 6, 25, 1.50, '一食堂', '一楼02号', '经典,快捷', '皮薄馅大的鲜肉包子，咬一口满嘴肉香', 1),
('鸡蛋煎饼', 'breakfast', '主食', 220, 10, 12, 20, 4.00, '一食堂', '一楼03号', '营养,快捷', '鸡蛋+薄脆+生菜，营养均衡的早餐选择', 1),
('豆浆油条', 'breakfast', '主食', 350, 8, 15, 45, 5.00, '二食堂', '早餐区', '经典,传统', '现磨豆浆配酥脆油条，经典中式早餐', 1),

-- 午餐
('红烧肉', 'lunch', '肉类', 380, 18, 28, 15, 12.00, '一食堂', '二楼01号', '下饭,经典', '肥瘦相间的红烧肉，入口即化', 1),
('宫保鸡丁', 'lunch', '肉类', 280, 15, 15, 20, 10.00, '一食堂', '二楼02号', '微辣,下饭', '鸡丁香嫩，花生酥脆，宫保味浓郁', 1),
('清炒时蔬', 'lunch', '蔬菜', 80, 3, 5, 8, 3.00, '一食堂', '二楼03号', '健康,素食', '新鲜时令蔬菜，清淡爽口', 1),
('番茄炒蛋', 'lunch', '蔬菜', 150, 8, 10, 12, 6.00, '二食堂', '自选区', '家常,营养', '番茄酸甜可口，鸡蛋嫩滑', 1),
('糖醋里脊', 'lunch', '肉类', 320, 16, 18, 28, 14.00, '二食堂', '特色区', '酸甜,开胃', '外酥里嫩，酸甜适口', 1),
('土豆烧牛肉', 'lunch', '肉类', 350, 22, 20, 18, 16.00, '三食堂', '风味区', '硬菜,滋补', '牛肉软烂入味，土豆绵密可口', 1),

-- 晚餐
('蒜蓉西兰花', 'dinner', '蔬菜', 90, 4, 6, 8, 5.00, '一食堂', '二楼03号', '健康,素食', '西兰花脆嫩，蒜香浓郁', 1),
('酸菜鱼', 'dinner', '肉类', 250, 20, 15, 10, 18.00, '三食堂', '特色区', '开胃,营养', '鱼肉鲜嫩，酸菜爽口', 1),
('凉皮', 'dinner', '主食', 200, 5, 8, 30, 8.00, '三食堂', '小吃区', '清爽,快捷', '凉拌凉皮，配上黄瓜丝和酱汁', 1),

-- 减脂餐
('鸡胸肉沙拉', 'lunch', '肉类', 180, 28, 5, 8, 18.00, '二食堂', '健康餐区', '减脂,高蛋白', '水煮鸡胸肉配生菜沙拉，少油少盐', 1),
('糙米饭套餐', 'lunch', '主食', 280, 8, 3, 55, 10.00, '二食堂', '健康餐区', '减脂,粗粮', '糙米饭配清炒蔬菜，健康低脂', 1);

-- =============================================
-- 创建用户（默认密码：123456，使用MD5加密）
-- =============================================
INSERT INTO `user` (`username`, `password`, `phone`, `nickname`, `city`, `status`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', '管理员', '北京', 1),
('test', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', '测试用户', '上海', 1);

-- =============================================
-- 初始化用户健康档案
-- =============================================
INSERT INTO `user_health` (`user_id`, `height`, `weight`, `bmi`, `age`, `gender`, `diet_goal`, `daily_calories`) VALUES
(1, 175.0, 70.0, 22.86, 20, 'M', 'balance', 2000),
(2, 165.0, 55.0, 20.20, 19, 'F', 'balance', 1600);

-- =============================================
-- 初始化用户技能配置
-- =============================================
INSERT INTO `user_skill_config` (`user_id`, `skill_key`, `skill_name`, `enabled`) VALUES
(1, 'weather', '天气查询', 1),
(1, 'outfit', '穿搭建议', 1),
(1, 'diet', '饮食推荐', 1),
(1, 'course', '课程管理', 1),
(1, 'cost', '智能记账', 1),
(1, 'note', 'AI笔记', 1),
(1, 'notification', '消息推送', 1),
(1, 'rag', 'RAG问答', 1),
(2, 'weather', '天气查询', 1),
(2, 'outfit', '穿搭建议', 1),
(2, 'diet', '饮食推荐', 1),
(2, 'course', '课程管理', 1),
(2, 'cost', '智能记账', 1),
(2, 'note', 'AI笔记', 1),
(2, 'notification', '消息推送', 1),
(2, 'rag', 'RAG问答', 1);

-- =============================================
-- 初始化课程数据
-- =============================================
INSERT INTO `course_schedule` (`user_id`, `name`, `teacher`, `location`, `weekday`, `start_time`, `end_time`, `week_start`, `week_end`, `course_type`, `semester`) VALUES
(1, '高等数学', '张教授', '教学楼A101', 1, '08:00:00', '09:40:00', 1, 16, 'required', '2024春'),
(1, '大学英语', '李老师', '外语楼301', 1, '10:00:00', '11:40:00', 1, 16, 'required', '2024春'),
(1, '计算机基础', '王老师', '实验楼201', 2, '08:00:00', '09:40:00', 1, 16, 'required', '2024春'),
(1, '体育与健康', '刘老师', '体育馆', 3, '14:00:00', '15:40:00', 1, 16, 'public', '2024春'),
(1, '高等数学', '张教授', '教学楼A101', 3, '08:00:00', '09:40:00', 1, 16, 'required', '2024春'),
(1, '线性代数', '陈教授', '教学楼B205', 4, '10:00:00', '11:40:00', 1, 16, 'required', '2024春'),
(1, '大学物理', '赵老师', '实验楼301', 5, '08:00:00', '09:40:00', 1, 16, 'required', '2024春');

-- =============================================
-- 初始化消费记录（模拟数据）
-- =============================================
INSERT INTO `cost_record` (`user_id`, `amount`, `category`, `source`, `merchant_name`, `trade_time`) VALUES
(1, 12.50, 'food', 'wechat', '一食堂二楼', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 5.00, 'transport', 'alipay', '地铁公交', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 88.00, 'shopping', 'wechat', '淘宝店铺', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 35.00, 'entertainment', 'wechat', '电影院', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 15.00, 'food', 'alipay', '水果店', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 8.00, 'study', 'wechat', '书店', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 25.00, 'food', 'alipay', '外卖', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- =============================================
-- 初始化消息通知表（Quartz需要）
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '通知类型（system系统/morning早安/course课程/cost_report消费报告/note笔记）',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT DEFAULT NULL COMMENT '通知内容',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0未读，1已读）',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息通知表';

-- =============================================
-- 初始化用户消息记录
-- =============================================
INSERT INTO `sys_notification` (`user_id`, `type`, `title`, `content`, `status`) VALUES
(1, 'system', '欢迎使用青途智伴', '欢迎使用青途智伴AI生活助手，让我们一起开启智能校园生活！', 0),
(1, 'morning', '今日天气提醒', '今日天气：晴，15-25°C，适合户外活动。', 0),
(1, 'course', '课程提醒', '您有课程《高等数学》将在15分钟后开始，地点：教学楼A101', 0);

-- =============================================
-- 初始化聊天会话表
-- =============================================
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- =============================================
-- 初始化聊天消息表
-- =============================================
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- =============================================
-- 异步任务表（用于RAG问答异步处理）
-- =============================================
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `question` VARCHAR(1000) NOT NULL COMMENT '用户问题',
    `session_id` VARCHAR(50) DEFAULT NULL COMMENT '会话ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'processing' COMMENT '状态（processing/completed/failed）',
    `answer` TEXT DEFAULT NULL COMMENT '回答内容',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';