package com.qingtu.agent.tool;

import lombok.Data;

import java.util.Map;

/**
 * 工具调用请求
 */
@Data
public class ToolCall {
    private String id;
    private String name;
    private Map<String, Object> arguments;
    private int retryCount;

    public ToolCall() {}

    public ToolCall(String name, Map<String, Object> arguments) {
        this.name = name;
        this.arguments = arguments;
        this.id = java.util.UUID.randomUUID().toString();
        this.retryCount = 0;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
