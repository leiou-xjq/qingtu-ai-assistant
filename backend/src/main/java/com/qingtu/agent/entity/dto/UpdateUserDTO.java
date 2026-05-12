package com.qingtu.agent.entity.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 更新用户信息DTO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class UpdateUserDTO implements Serializable {

    private String nickname;

    private String avatar;

    private String email;

    private String city;

    private String school;

    private Integer gender;

    private String semesterStart;

    private Integer totalWeeks;

    private String dingtalkWebhook;

    private String workweixinWebhook;
}