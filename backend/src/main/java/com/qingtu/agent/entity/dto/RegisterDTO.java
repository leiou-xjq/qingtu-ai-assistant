package com.qingtu.agent.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.io.Serializable;

@Data
public class RegisterDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    private String username;

    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    private String school;

    private String city;

    private Integer age;

    private String gender;

    private Double height;

    private Double weight;

    private Double activityLevel;
}