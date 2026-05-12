package com.qingtu.agent.tool;

import lombok.Data;

/**
 * 工具执行结果
 */
@Data
public class ToolResult {
    private String id;
    private String toolName;
    private boolean success;
    private Object data;
    private String errorMessage;
    private long executionTimeMs;

    public static ToolResult success(String id, String toolName, Object data, long executionTimeMs) {
        ToolResult result = new ToolResult();
        result.id = id;
        result.toolName = toolName;
        result.success = true;
        result.data = data;
        result.executionTimeMs = executionTimeMs;
        return result;
    }

    public static ToolResult failed(String id, String toolName, String errorMessage, long executionTimeMs) {
        ToolResult result = new ToolResult();
        result.id = id;
        result.toolName = toolName;
        result.success = false;
        result.errorMessage = errorMessage;
        result.executionTimeMs = executionTimeMs;
        return result;
    }
}
