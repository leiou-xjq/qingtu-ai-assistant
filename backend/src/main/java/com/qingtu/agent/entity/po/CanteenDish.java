package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食堂菜品实体类
 * 
 * 对应数据库表：canteen_dish
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("canteen_dish")
public class CanteenDish implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 菜品名称
     */
    private String name;

    /**
     * 菜品类型（breakfast早餐/lunch午餐/dinner晚餐/snack夜宵）
     */
    private String type;

    /**
     * 菜品种类（主食/肉类/蔬菜/汤类/水果等）
     */
    private String category;

    /**
     * 热量（千卡）
     */
    private Integer calories;

    /**
     * 蛋白质（克）
     */
    private BigDecimal protein;

    /**
     * 脂肪（克）
     */
    private BigDecimal fat;

    /**
     * 碳水化合物（克）
     */
    private BigDecimal carbs;

    /**
     * 价格（元）
     */
    private BigDecimal price;

    /**
     * 所属食堂
     */
    private String canteen;

    /**
     * 窗口位置
     */
    @TableField("`window`")
    private String window;

    /**
     * 标签（减脂/增肌/素食/辣等）
     */
    private String tags;

    /**
     * 菜品图片URL
     */
    private String imageUrl;

    /**
     * 菜品描述
     */
    private String description;

    /**
     * 营养成分详情（JSON格式）
     */
    private String nutritionInfo;

    /**
     * 状态（0下架，1在售）
     */
    private Integer status;

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