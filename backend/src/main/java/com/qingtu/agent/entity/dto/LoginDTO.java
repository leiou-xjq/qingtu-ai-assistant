package com.qingtu.agent.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 用户登录DTO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class LoginDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}