package com.qingtu.agent.mcp.server.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * MCP 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpAuditService {

    private final McpAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_ARG_LENGTH = 2000;
    private static final int MAX_RESULT_LENGTH = 5000;

    /**
     * 记录工具调用日志（异步）
     */
    @Async("auditExecutor")
    public void logToolCall(String toolName, String category, Long userId,
                            Object arguments, Object result, String status,
                            Long durationMs, String ipAddress, String errorMessage) {
        try {
            McpAuditLog auditLog = new McpAuditLog();
            auditLog.setUserId(userId);
            auditLog.setToolName(toolName);
            auditLog.setCategory(category);
            auditLog.setStatus(status);
            auditLog.setDurationMs(durationMs);
            auditLog.setIpAddress(ipAddress);
            auditLog.setErrorMessage(errorMessage);

            // 截断参数和结果
            auditLog.setArguments(truncate(toolName, arguments, MAX_ARG_LENGTH));
            auditLog.setResult(truncate(toolName, result, MAX_RESULT_LENGTH));

            auditLogMapper.insert(auditLog);
            log.debug("MCP工具调用审计日志已记录: tool={}, userId={}", toolName, userId);
        } catch (Exception e) {
            log.error("记录MCP审计日志失败", e);
        }
    }

    /**
     * 查询用户调用统计
     */
    public long countUserCalls(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.selectCount(new LambdaQueryWrapper<McpAuditLog>()
                .eq(McpAuditLog::getUserId, userId)
                .between(McpAuditLog::getCreatedAt, startTime, endTime));
    }

    /**
     * 查询工具调用统计
     */
    public long countToolCalls(String toolName, LocalDateTime startTime, LocalDateTime endTime) {
        return auditLogMapper.selectCount(new LambdaQueryWrapper<McpAuditLog>()
                .eq(McpAuditLog::getToolName, toolName)
                .between(McpAuditLog::getCreatedAt, startTime, endTime));
    }

    /**
     * 清理过期日志
     */
    public int cleanupOldLogs(int retentionDays) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        return auditLogMapper.delete(new LambdaQueryWrapper<McpAuditLog>()
                .lt(McpAuditLog::getCreatedAt, cutoffTime));
    }

    private String truncate(String context, Object obj, int maxLength) {
        if (obj == null) return "";
        try {
            String json = objectMapper.writeValueAsString(obj);
            if (json.length() > maxLength) {
                return json.substring(0, maxLength) + "...[TRUNCATED]";
            }
            return json;
        } catch (Exception e) {
            return obj.toString().substring(0, Math.min(obj.toString().length(), maxLength));
        }
    }
}
