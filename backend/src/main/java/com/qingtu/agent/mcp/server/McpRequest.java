package com.qingtu.agent.mcp.server;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP 请求上下文
 */
@Data
public class McpRequest {
    private String method;           // 调用的工具名称
    private Map<String, Object> params;  // 工具参数
    private Long userId;            // 用户ID
    private String sessionId;       // 会话ID
    private Long timestamp;         // 时间戳
    private String requestId;       // 请求唯一ID
    private Map<String, String> headers;  // 请求头
}
