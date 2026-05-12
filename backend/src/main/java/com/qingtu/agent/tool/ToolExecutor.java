package com.qingtu.agent.tool;

/**
 * 工具执行器接口
 */
public interface ToolExecutor {

    /**
     * 获取工具名称（全局唯一）
     */
    String getName();

    /**
     * 获取工具描述
     */
    String getDescription();

    /**
     * 获取工具分类
     */
    default String getCategory() {
        return "general";
    }

    /**
     * 执行工具
     * @param arguments 工具参数（JSON 转 Map）
     * @return 执行结果
     */
    ToolDefinition.ExecuteResult execute(java.util.Map<String, Object> arguments);

    /**
     * 是否启用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 执行超时（毫秒）
     */
    default long getTimeoutMs() {
        return 30000;
    }
}
