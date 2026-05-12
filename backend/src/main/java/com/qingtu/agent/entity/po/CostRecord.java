package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消费记录实体类
 * 
 * 对应数据库表：cost_record
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("cost_record")
public class CostRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消费记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消费金额（元）
     */
    private BigDecimal amount;

    /**
     * 消费分类（food饮食/transport交通/entertainment娱乐/shopping购物/life生活/study学习/other其他）
     */
    private String category;

    /**
     * 来源（wechat微信/alipay支付宝/manual手动）
     */
    private String source;

    /**
     * 交易单号
     */
    private String tradeNo;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 交易时间
     */
    private LocalDateTime tradeTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 标签
     */
    private String tags;

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