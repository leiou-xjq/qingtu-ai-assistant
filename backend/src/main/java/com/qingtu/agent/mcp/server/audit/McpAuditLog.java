package com.qingtu.agent.mcp.server.audit;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 工具调用审计日志
 */
@Data
@TableName("mcp_audit_log")
public class McpAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String toolName;

    private String category;  // school/rag/doc/weather

    private String arguments;  // JSON（截断）

    private String result;  // JSON（截断）

    private String status;  // SUCCESS/FAILED

    private Long durationMs;

    private String ipAddress;

    private String userAgent;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
