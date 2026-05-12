-- 初始化用户（密码：123456）
INSERT INTO user (username, password, phone, nickname, city, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', '管理员', '北京', 1),
('test', 'e10adc3949ba59abbe56e057f20f883e', '13900139000', '测试用户', '上海', 1);

-- 初始化健康档案
INSERT INTO user_health (user_id, height, weight, bmi, age, gender, diet_goal, daily_calories) VALUES
(1, 175.0, 70.0, 22.86, 20, 'M', 'balance', 2000),
(2, 165.0, 55.0, 20.20, 19, 'F', 'balance', 1600);

-- 初始化技能配置
INSERT INTO user_skill_config (user_id, skill_key, skill_name, enabled) VALUES
(1, 'weather', '天气查询', 1),
(1, 'outfit', '穿搭建议', 1),
(1, 'diet', '饮食推荐', 1),
(1, 'course', '课程管理', 1),
(1, 'cost', '智能记账', 1),
(1, 'note', 'AI笔记', 1),
(1, 'notification', '消息推送', 1),
(1, 'rag', 'RAG问答', 1);

-- 初始化任务
INSERT INTO sys_task_config (task_key, task_name, task_group, cron_expression, enabled, description) VALUES
('morningPush', '每日早安推送', 'PUSH', '0 30 7 * * ?', 1, '每日7:30推送'),
('courseReminder', '课前提醒', 'REMINDER', '0 45 * * * ?', 1, '课前提醒');

-- 初始化菜品
INSERT INTO canteen_dish (name, type, category, calories, protein, price, canteen, status) VALUES
('紫薯粥', 'breakfast', '主食', 120, 2, 2.00, '一食堂', 1),
('红烧肉', 'lunch', '肉类', 380, 18, 12.00, '一食堂', 1);

-- 初始化通知
INSERT INTO sys_notification (user_id, type, title, content, status) VALUES
(1, 'system', '欢迎使用', '欢迎使用青途智伴！', 0);