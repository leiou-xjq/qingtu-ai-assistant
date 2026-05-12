package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统消息通知实体类
 * 
 * 对应数据库表：sys_notification
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("sys_notification")
public class SysNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知类型（system系统/morning早安/course课程/cost_report消费报告/note笔记）
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 跳转页面
     */
    private String targetPage;

    /**
     * 详情ID
     */
    private String detailId;

    /**
     * 预生成缓存内容
     */
    private String cachedContent;

    /**
     * 状态（0未读，1已读）
     */
    private Integer status;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除（0未删除，1已删除）
     */
    @TableLogic
    private Integer deleted;
}