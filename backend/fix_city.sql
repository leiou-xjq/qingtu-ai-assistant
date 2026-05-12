-- 更新用户城市字段
UPDATE user SET city='北京' WHERE username='admin';

-- 确认更新结果
SELECT username, city FROM user WHERE username='admin';