package com.qingtu.agent.mcp.server;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 响应结果
 */
@Data
@Builder
public class McpResponse {
    private String requestId;       // 请求ID
    private boolean success;       // 是否成功
    private Object data;           // 返回数据
    private String errorCode;      // 错误码
    private String errorMessage;   // 错误信息
    private long executionTimeMs;  // 执行耗时
    private LocalDateTime timestamp;
    private McpMetadata metadata;  // 元数据

    @Data
    @Builder
    public static class McpMetadata {
        private String toolName;        // 工具名称
        private String category;       // 工具分类
        private String userId;         // 用户ID
        private String permission;      // 权限等级
        private String version;        // MCP版本
    }

    public static McpResponse success(String requestId, Object data, long executionTimeMs) {
        return McpResponse.builder()
                .requestId(requestId)
                .success(true)
                .data(data)
                .executionTimeMs(executionTimeMs)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static McpResponse error(String requestId, String errorCode, String errorMessage, long executionTimeMs) {
        return McpResponse.builder()
                .requestId(requestId)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .executionTimeMs(executionTimeMs)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
