package com.qingtu.agent.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 修改密码DTO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class ChangePasswordDTO implements Serializable {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}