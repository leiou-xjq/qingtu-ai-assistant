-- 添加通知表跳转字段
ALTER TABLE sys_notification ADD COLUMN target_page VARCHAR(128) DEFAULT NULL COMMENT '跳转页面';
ALTER TABLE sys_notification ADD COLUMN detail_id VARCHAR(64) DEFAULT NULL COMMENT '详情ID';
ALTER TABLE sys_notification ADD COLUMN cached_content TEXT COMMENT '预生成缓存内容';
