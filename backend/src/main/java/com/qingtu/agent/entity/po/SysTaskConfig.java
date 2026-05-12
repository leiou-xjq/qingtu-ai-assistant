package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务配置实体类
 * 
 * 对应数据库表：sys_task_config
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("sys_task_config")
public class SysTaskConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务标识（唯一）
     */
    private String taskKey;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务分组
     */
    private String taskGroup;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * ���否启用（0禁用，1启用）
     */
    private Integer enabled;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务参数（JSON格式）
     */
    private String params;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastRunTime;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextRunTime;

    /**
     * 执行次数
     */
    private Integer runCount;

    /**
     * 成功次数
     */
    private Integer successCount;

    /**
     * 失败次数
     */
    private Integer failCount;

    /**
     * 平均执行时长（毫秒）
     */
    private Integer avgDuration;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除（0未删除，1已删除）
     */
    @TableLogic
    private Integer deleted;
}