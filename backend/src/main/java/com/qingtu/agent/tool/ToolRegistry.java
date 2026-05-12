package com.qingtu.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具注册表
 * 管理所有 ToolExecutor 实例，为 LLM Function Calling 提供工具定义和执行
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, ToolExecutor> toolMap = new LinkedHashMap<>();
    private final List<ToolExecutor> toolExecutors;

    public ToolRegistry(List<ToolExecutor> toolExecutors) {
        this.toolExecutors = toolExecutors;
        registerAll();
    }

    private void registerAll() {
        for (ToolExecutor executor : toolExecutors) {
            toolMap.put(executor.getName(), executor);
        }
        log.info("ToolRegistry 已注册 {} 个工具: {}", toolMap.size(), new ArrayList<>(toolMap.keySet()));
    }

    public Optional<ToolDefinition> getToolDefinition(String name) {
        ToolExecutor executor = toolMap.get(name);
        if (executor == null) return Optional.empty();

        ToolDefinition def = new ToolDefinition();
        def.setName(executor.getName());
        def.setDescription(executor.getDescription());
        def.setCategory(executor.getCategory());
        return Optional.of(def);
    }

    /**
     * 获取所有工具函数定义（OpenAI Function Calling 格式）
     */
    public List<Map<String, Object>> getFunctionDefinitions() {
        List<Map<String, Object>> defs = new ArrayList<>();
        for (ToolExecutor executor : toolMap.values()) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("name", executor.getName());
            def.put("description", executor.getDescription());
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("type", "object");
            params.put("properties", new LinkedHashMap<>());
            def.put("parameters", params);
            defs.add(def);
        }
        return defs;
    }

    public ToolResult executeTool(String toolName, Map<String, Object> arguments) {
        ToolExecutor executor = toolMap.get(toolName);
        if (executor == null) {
            log.warn("工具未注册: {}", toolName);
            return ToolResult.failed(toolName, toolName, "工具未注册: " + toolName, 0);
        }

        long startTime = System.currentTimeMillis();
        try {
            ToolDefinition.ExecuteResult result = executor.execute(arguments);
            long duration = System.currentTimeMillis() - startTime;
            return result.success()
                ? ToolResult.success(toolName, executor.getCategory(), result.data(), duration)
                : ToolResult.failed(toolName, executor.getCategory(), result.errorMessage(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("工具执行异常: tool={}, error={}", toolName, e.getMessage());
            return ToolResult.failed(toolName, executor.getCategory(), e.getMessage(), duration);
        }
    }

    public Map<String, ToolResult> executeTools(List<ToolCall> toolCalls) {
        Map<String, ToolResult> results = new HashMap<>();
        for (ToolCall call : toolCalls) {
            ToolResult result = executeTool(call.getName(), call.getArguments());
            results.put(call.getName(), result);
        }
        return results;
    }
}
