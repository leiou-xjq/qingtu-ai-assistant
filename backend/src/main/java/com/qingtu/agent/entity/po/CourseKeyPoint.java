package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 课程AI笔记实体类
 * 
 * 对应数据库表：course_key_point
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("course_key_point")
public class CourseKeyPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 笔记ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 教学周数
     */
    private Integer weekNum;

    /**
     * 课程日期
     */
    private LocalDate classDate;

    /**
     * 课程时间段
     */
    private String classTime;

    /**
     * 核心知识点（JSON格式）
     */
    private String corePoints;

    /**
     * 考试重点（JSON格式）
     */
    private String examPoints;

    /**
     * 难点（JSON格式）
     */
    private String difficultPoints;

    /**
     * 易错点（JSON格式）
     */
    @com.baomidou.mybatisplus.annotation.TableField("易错点")
    private String 易错点;

    /**
     * 复习指南
     */
    private String reviewGuide;

    /**
     * 课程总结
     */
    private String summary;

    /**
     * 生成AI模型
     */
    private String aiModel;

    /**
     * 使用Token数
     */
    private Integer tokensUsed;

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