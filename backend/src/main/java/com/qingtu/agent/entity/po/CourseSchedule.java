package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 课程表实体类
 * 
 * 对应数据库表：course_schedule
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("course_schedule")
public class CourseSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 课程ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 授课教师
     */
    private String teacher;

    /**
     * 上课地点
     */
    private String location;

    /**
     * 星期几（1=周一，7=周日）
     */
    private Integer weekday;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 起始周
     */
    private Integer weekStart;

    /**
     * 结束周
     */
    private Integer weekEnd;

    /**
     * 课程类型（required必修/elective选修/public公选）
     */
    private String courseType;

    /**
     * 日历颜色
     */
    private String color;

    /**
     * 是否开启提醒（0否，1是）
     */
    private Integer reminderEnabled;

    /**
     * 提前提醒分钟数
     */
    private Integer reminderMinutes;

    /**
     * 学期（如：2024春）
     */
    private String semester;

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