package com.qingtu.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字符")
    private String message;

    private String sessionId;

    private String traceId;
}