-- RAG网页抓取相关表结构变更

-- 1. 用户表新增学校相关字段
ALTER TABLE `user`
ADD COLUMN school_website VARCHAR(500) COMMENT '学校官网地址' AFTER nickname;

ALTER TABLE `user`
ADD COLUMN school_name VARCHAR(100) COMMENT '学校名称' AFTER school_website;

ALTER TABLE `user`
ADD COLUMN last_crawl_time DATETIME COMMENT '最后抓取时间' AFTER school_name;

-- 2. 爬取记录表
CREATE TABLE IF NOT EXISTS `rag_crawl_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `school_website` VARCHAR(500) NOT NULL COMMENT '学校官网地址',
    `school_name` VARCHAR(100) COMMENT '学校名称',
    `page_type` VARCHAR(50) COMMENT 'campus|announce|canteen|course|exam',
    `page_url` VARCHAR(500) COMMENT '被抓取的页面URL',
    `status` INT DEFAULT 0 COMMENT '0待抓取,1成功,2失败',
    `page_count` INT DEFAULT 0 COMMENT '抓取页面数',
    `error_msg` VARCHAR(500) COMMENT '错误信息',
    `crawl_time` DATETIME COMMENT '抓取时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_school` (`school_website`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG爬取记录表';

-- 3. RAG知识库表新增学校标识字段
ALTER TABLE `rag_knowledge`
ADD COLUMN school_website VARCHAR(500) COMMENT '学校官网地址' AFTER category;

ALTER TABLE `rag_knowledge`
ADD COLUMN school_name VARCHAR(100) COMMENT '学校名称' AFTER school_website;