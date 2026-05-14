package com.qingtu.agent.tool.callback;

import java.util.Map;

public interface ToolCallback {
    String getName();
    String getDescription();
    Map<String, Object> getInputSchema();
    ToolCallbackResult execute(Map<String, Object> input);
    default String getCategory() { return "general"; }
    default boolean isEnabled() { return true; }
    default long getTimeoutMs() { return 30000; }
}