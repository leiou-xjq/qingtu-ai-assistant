package com.qingtu.agent.tool;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具注册表
 * 管理AI Agent可调用的工具
 */
@Slf4j
@Data
@Component
public class ToolRegistry {

    private static final Map<String, ToolDefinition> TOOLS = new HashMap<>();

    public ToolRegistry() {
        registerDefaultTools();
    }

    private void registerDefaultTools() {
    }

    public Optional<ToolDefinition> getToolDefinition(String name) {
        return Optional.ofNullable(TOOLS.get(name));
    }

    public Map<String, ToolResult> executeTools(List<ToolCall> toolCalls) {
        Map<String, ToolResult> results = new HashMap<>();
        for (ToolCall call : toolCalls) {
            try {
                ToolResult result = executeTool(call.getName(), call.getArguments());
                results.put(call.getName(), result);
            } catch (Exception e) {
                log.error("工具执行失败: {}", call.getName(), e);
                results.put(call.getName(), ToolResult.failed(call.getName(), call.getName(), e.getMessage(), 0));
            }
        }
        return results;
    }

    public ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        log.info("执行工具: {}", toolName);
        return ToolResult.success(toolName, toolName, "工具执行成功", 0);
    }

    public Map<String, ToolResult> executeTools(String toolName, List<Map<String, Object>> arguments) {
        Map<String, ToolResult> results = new HashMap<>();
        for (Map<String, Object> args : arguments) {
            try {
                ToolResult result = executeTool(toolName, args);
                results.put(toolName, result);
            } catch (Exception e) {
                log.error("工具执行失败: {}", toolName, e);
                results.put(toolName, ToolResult.failed(toolName, toolName, e.getMessage(), 0));
            }
        }
        return results;
    }
}
