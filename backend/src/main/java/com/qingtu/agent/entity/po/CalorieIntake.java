package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("calorie_intake")
public class CalorieIntake implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 餐食类型：breakfast/lunch/dinner
     */
    private String mealType;

    /**
     * 用户输入的食物描述
     */
    private String foodInput;

    /**
     * 估算的热量（大卡）
     */
    private Integer estimatedCalories;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}