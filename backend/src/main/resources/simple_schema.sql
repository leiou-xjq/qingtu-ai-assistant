-- User table
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20),
    `nickname` VARCHAR(50),
    `avatar` VARCHAR(500),
    `email` VARCHAR(100),
    `city` VARCHAR(50) DEFAULT 'Beijing',
    `semester_start` DATE,
    `total_weeks` INT DEFAULT 16,
    `dingtalk_webhook` VARCHAR(500),
    `workweixin_webhook` VARCHAR(500),
    `role` VARCHAR(20) DEFAULT 'user',
    `status` TINYINT DEFAULT 1,
    `last_login_time` DATETIME,
    `last_login_ip` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User health table
DROP TABLE IF EXISTS `user_health`;
CREATE TABLE `user_health` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `height` DECIMAL(5,1),
    `weight` DECIMAL(5,1),
    `bmi` DECIMAL(4,2),
    `age` INT,
    `gender` CHAR(1),
    `activity_level` DECIMAL(2,1) DEFAULT 1.2,
    `diet_goal` VARCHAR(20) DEFAULT 'balance',
    `daily_calories` INT DEFAULT 1500,
    `taboo_food` VARCHAR(500),
    `taste_preference` VARCHAR(500),
    `health_suggestion` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Canteen dish table
DROP TABLE IF EXISTS `canteen_dish`;
CREATE TABLE `canteen_dish` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `category` VARCHAR(50),
    `calories` INT DEFAULT 0,
    `protein` DECIMAL(5,1) DEFAULT 0,
    `fat` DECIMAL(5,1) DEFAULT 0,
    `carbs` DECIMAL(5,1) DEFAULT 0,
    `price` DECIMAL(8,2) DEFAULT 0,
    `canteen` VARCHAR(50),
    `window` VARCHAR(50),
    `tags` VARCHAR(200),
    `image_url` VARCHAR(500),
    `description` VARCHAR(500),
    `nutrition_info` TEXT,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_type` (`type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cost record table
DROP TABLE IF EXISTS `cost_record`;
CREATE TABLE `cost_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(10,2) NOT NULL,
    `category` VARCHAR(20) NOT NULL,
    `source` VARCHAR(20) DEFAULT 'manual',
    `trade_no` VARCHAR(100),
    `merchant_name` VARCHAR(100),
    `trade_time` DATETIME,
    `remark` VARCHAR(500),
    `tags` VARCHAR(200),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_trade_time` (`trade_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Course schedule table
DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `teacher` VARCHAR(50),
    `location` VARCHAR(100),
    `weekday` TINYINT NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `week_start` INT DEFAULT 1,
    `week_end` INT DEFAULT 16,
    `course_type` VARCHAR(20) DEFAULT 'required',
    `color` VARCHAR(20) DEFAULT '#3B82F6',
    `reminder_enabled` TINYINT DEFAULT 1,
    `reminder_minutes` INT DEFAULT 15,
    `semester` VARCHAR(20),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_user_weekday` (`user_id`, `weekday`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Course key point table
DROP TABLE IF EXISTS `course_key_point`;
CREATE TABLE `course_key_point` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `course_name` VARCHAR(100),
    `week_num` INT NOT NULL,
    `class_date` DATE NOT NULL,
    `class_time` VARCHAR(20),
    `core_points` TEXT,
    `exam_points` TEXT,
    `difficult_points` TEXT,
    `common_errors` TEXT,
    `review_guide` TEXT,
    `summary` TEXT,
    `ai_model` VARCHAR(50),
    `tokens_used` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_user_course` (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Task config table
DROP TABLE IF EXISTS `sys_task_config`;
CREATE TABLE `sys_task_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `task_key` VARCHAR(50) NOT NULL UNIQUE,
    `task_name` VARCHAR(100) NOT NULL,
    `task_group` VARCHAR(50) DEFAULT 'DEFAULT',
    `cron_expression` VARCHAR(100),
    `enabled` TINYINT DEFAULT 1,
    `description` VARCHAR(500),
    `params` TEXT,
    `last_run_time` DATETIME,
    `next_run_time` DATETIME,
    `run_count` INT DEFAULT 0,
    `success_count` INT DEFAULT 0,
    `fail_count` INT DEFAULT 0,
    `avg_duration` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- RAG knowledge table
DROP TABLE IF EXISTS `rag_knowledge`;
CREATE TABLE `rag_knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `category` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `source` VARCHAR(100),
    `tags` VARCHAR(200),
    `vector_id` VARCHAR(100),
    `chunk_count` INT DEFAULT 1,
    `status` TINYINT DEFAULT 1,
    `index_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User skill config table
DROP TABLE IF EXISTS `user_skill_config`;
CREATE TABLE `user_skill_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `skill_key` VARCHAR(50) NOT NULL,
    `skill_name` VARCHAR(100),
    `enabled` TINYINT DEFAULT 1,
    `config_json` TEXT,
    `last_used_time` DATETIME,
    `use_count` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_user_skill` (`user_id`, `skill_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notification table
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `status` TINYINT DEFAULT 0,
    `read_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert sample data
INSERT INTO `user` (`username`, `password`, `phone`, `nickname`, `city`, `status`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', 'Admin', 'Beijing', 1),
('test', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', 'Test User', 'Shanghai', 1);

INSERT INTO `user_health` (`user_id`, `height`, `weight`, `bmi`, `age`, `gender`, `diet_goal`, `daily_calories`) VALUES
(1, 175.0, 70.0, 22.86, 20, 'M', 'balance', 2000),
(2, 165.0, 55.0, 20.20, 19, 'F', 'balance', 1600);

INSERT INTO `canteen_dish` (`name`, `type`, `category`, `calories`, `protein`, `fat`, `carbs`, `price`, `canteen`, `window`, `tags`) VALUES
('Steamed buns', 'breakfast', 'staple', 180, 8, 6, 25, 1.50, 'Canteen 1', 'Window 1', 'classic'),
('Soy milk', 'breakfast', 'drink', 80, 4, 3, 8, 2.00, 'Canteen 1', 'Window 1', 'healthy'),
('Braised pork', 'lunch', 'meat', 380, 18, 28, 15, 12.00, 'Canteen 1', 'Window 2', 'classic'),
('Stir-fried vegetables', 'lunch', 'vegetable', 80, 3, 5, 8, 3.00, 'Canteen 1', 'Window 3', 'healthy,vegetarian'),
('Tomato scrambled eggs', 'lunch', 'vegetable', 150, 8, 10, 12, 6.00, 'Canteen 2', 'Area 1', 'home-style,nutritious'),
('Kung Pao chicken', 'dinner', 'meat', 280, 15, 15, 20, 10.00, 'Canteen 1', 'Window 4', 'spicy'),
('Cold noodles', 'dinner', 'staple', 200, 5, 8, 30, 8.00, 'Canteen 3', 'Snack Area', 'refreshing'),
('Grilled chicken breast', 'lunch', 'meat', 180, 28, 5, 8, 18.00, 'Canteen 2', 'Healthy Zone', 'low-fat,high-protein');

INSERT INTO `course_schedule` (`user_id`, `name`, `teacher`, `location`, `weekday`, `start_time`, `end_time`, `week_start`, `week_end`, `course_type`, `semester`) VALUES
(1, 'Advanced Mathematics', 'Prof. Zhang', 'Building A101', 1, '08:00:00', '09:40:00', 1, 16, 'required', '2024 Spring'),
(1, 'College English', 'Teacher Li', 'Foreign Language Building 301', 1, '10:00:00', '11:40:00', 1, 16, 'required', '2024 Spring'),
(1, 'Computer Basics', 'Teacher Wang', 'Lab Building 201', 2, '08:00:00', '09:40:00', 1, 16, 'required', '2024 Spring'),
(1, 'Physical Education', 'Teacher Liu', 'Gymnasium', 3, '14:00:00', '15:40:00', 1, 16, 'public', '2024 Spring'),
(1, 'Linear Algebra', 'Prof. Chen', 'Building B205', 4, '10:00:00', '11:40:00', 1, 16, 'required', '2024 Spring'),
(1, 'College Physics', 'Teacher Zhao', 'Lab Building 301', 5, '08:00:00', '09:40:00', 1, 16, 'required', '2024 Spring');

INSERT INTO `sys_task_config` (`task_key`, `task_name`, `task_group`, `cron_expression`, `enabled`, `description`, `params`) VALUES
('morningPush', 'Daily Morning Push', 'PUSH', '0 30 7 * * ?', 1, 'Daily weather and outfit push at 7:30', '{"pushHour":7,"pushMinute":30}'),
('courseReminder', 'Course Reminder', 'REMINDER', '0 45 * * * ?', 1, '15 minutes before class reminder', '{"reminderMinutes":15}'),
('courseNote', 'Course Note Generation', 'AI', '0 5,50 * * * ?', 1, 'Generate AI notes 5 minutes after class', '{"generateDelay":5}'),
('dailySummary', 'Daily Note Summary', 'PUSH', '0 0 18 * * ?', 1, 'Daily note summary at 18:00', '{"summaryHour":18}'),
('monthlyReport', 'Monthly Cost Report', 'AI', '0 0 0 28 * ?', 1, 'Monthly consumption analysis on the 28th', '{"reportDay":28}');

INSERT INTO `rag_knowledge` (`category`, `title`, `content`, `source`, `tags`, `status`) VALUES
('course', 'Math Overview', 'Advanced Mathematics includes: 1. Functions and Limits 2. Derivatives and Differentials 3. Applications of Mean Value Theorems 4. Indefinite Integrals 5. Definite Integrals 6. Applications of Definite Integrals 7. Differential Equations 8. Multivariable Calculus 9. Double Integrals 10. Infinite Series', 'Academic Affairs', 'math,required,outline', 1),
('course', 'English Learning Points', 'College English includes: 1. Listening and Speaking 2. Reading Comprehension 3. Writing Methods 4. Translation Skills 5. Vocabulary Building 6. Grammar Review', 'Foreign Language College', 'english,required,language', 1),
('canteen', 'Campus Dining Guide', 'Campus dining options: Canteen 1: Breakfast, noodles; Canteen 2: Self-service meals; Canteen 3: Specialty snacks. Operating hours: 6:30-20:30', 'Logistics Group', 'dining,canteen,guide', 1),
('campus', 'Library Usage Guide', 'Library services: Opening hours 7:00-22:30. Floor 1: Study area; Floor 2: Borrowing area; Floor 3: Professional books; Floor 4: E-reading room. Borrowing rules: Undergraduates can borrow 10 books for 30 days.', 'Library', 'library,borrowing,study', 1),
('exam', 'Exam Preparation Guide', 'Exam preparation strategy: 1. Review course framework 2. Organize class notes 3. Complete homework 4. Review key chapters 5. Practice mock exams. Time arrangement: Start reviewing 2 weeks in advance.', 'Student Affairs', 'exam,review,strategy', 1);

INSERT INTO `user_skill_config` (`user_id`, `skill_key`, `skill_name`, `enabled`) VALUES
(1, 'weather', 'Weather Query', 1),
(1, 'outfit', 'Outfit Suggestion', 1),
(1, 'diet', 'Diet Recommendation', 1),
(1, 'course', 'Course Management', 1),
(1, 'cost', 'Smart Accounting', 1),
(1, 'note', 'AI Notes', 1),
(1, 'notification', 'Message Push', 1),
(1, 'rag', 'RAG Q&A', 1),
(2, 'weather', 'Weather Query', 1),
(2, 'outfit', 'Outfit Suggestion', 1),
(2, 'diet', 'Diet Recommendation', 1),
(2, 'course', 'Course Management', 1),
(2, 'cost', 'Smart Accounting', 1),
(2, 'note', 'AI Notes', 1),
(2, 'notification', 'Message Push', 1),
(2, 'rag', 'RAG Q&A', 1);

INSERT INTO `sys_notification` (`user_id`, `type`, `title`, `content`, `status`) VALUES
(1, 'system', 'Welcome', 'Welcome to Qingtu AI Assistant!', 0),
(1, 'morning', 'Weather Reminder', 'Today weather: Sunny, 15-25C, suitable for outdoor activities.', 0),
(1, 'course', 'Course Reminder', 'You have a course Advanced Mathematics starting in 15 minutes. Location: Building A101', 0);