package com.qingtu.agent.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * 对应数据库表：user
 * 
 * @author 青途智伴技术团队
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 学校名称
     */
private String school;

    private String schoolName;

    private Integer gender;

    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 所在城市（用于天气查询）
     */
    private String city;

    /**
     * 身高（cm）
     */
    private Double height;

    /**
     * 体重（kg）
     */
    private Double weight;

    /**
     * 口味偏好
     */
    private String tastePreference;

    /**
     * 学期开始日期
     */
    private LocalDate semesterStart;

    /**
     * 教学总周数
     */
    private Integer totalWeeks;

    /**
     * 钉钉webhook地址
     */
    private String dingtalkWebhook;

    /**
     * 企业微信webhook地址
     */
    private String workweixinWebhook;

    /**
     * uni-push 客户端ID
     */
    private String clientId;

    /**
     * 角色（user/admin）
     */
    private String role;

    /**
     * 状态（0禁用，1正常）
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

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