package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户健康档案实体类
 * 
 * 对应数据库表：user_health
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("user_health")
public class UserHealth implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 健康档案ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 身高（厘米）
     */
    private BigDecimal height;

    /**
     * 体重（公斤）
     */
    private BigDecimal weight;

    /**
     * BMI值
     */
    private BigDecimal bmi;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别（M男，F女）
     */
    private String gender;

    /**
     * 活动水平（1.2久坐/1.375轻量/1.55中等/1.75活跃）
     */
    private BigDecimal activityLevel;

    /**
     * 饮食目标（lose减脂/gain增肌/balance均衡）
     */
    private String dietGoal;

    /**
     * 每日建议摄入卡路里
     */
    private Integer dailyCalories;

    /**
     * 饮食忌口（逗号分隔）
     */
    private String tabooFood;

    /**
     * 口味偏好（逗号分隔）
     */
    private String tastePreference;

    /**
     * 健康建议
     */
    private String healthSuggestion;

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