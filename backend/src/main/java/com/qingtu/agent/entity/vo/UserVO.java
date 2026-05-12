package com.qingtu.agent.entity.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户信息VO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private String city;

    private String semesterStart;

    private Integer totalWeeks;

    private String role;

    private String school;

    private Integer gender;

    private String token;
}