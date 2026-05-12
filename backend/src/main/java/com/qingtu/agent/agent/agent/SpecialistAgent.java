package com.qingtu.agent.agent.agent;

import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * SpecialistAgent基类
 * 所有业务Agent的父类，统一上下文传递和结果处理
 */
@Slf4j
public abstract class SpecialistAgent {

    protected static final int MAX_RETRIES = 3;

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        return execute(action, context, params, Map.of());
    }

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params, Map<String, String> files) {
        String taskId = java.util.UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();

        try {
            log.debug("Agent执行: type={}, action={}, userId={}", getAgentType(), action, context.getUserId());
            return doExecute(taskId, action, context, params, files, correlationId);
        } catch (Exception e) {
            log.error("Agent执行失败: type={}, action={}", getAgentType(), action, e);
            return ResultMessage.failure(taskId, getAgentType(), action, e.getMessage(), correlationId, context.getUserId());
        }
    }

    protected abstract ResultMessage doExecute(String taskId, String action, UserContext context, Map<String, Object> params, Map<String, String> files, String correlationId);

    protected abstract String getAgentType();

    protected Long getUserId(Map<String, Object> params) {
        Object userId = params.get("_userId");
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    protected String getStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }

    protected String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        String value = getStringParam(params, key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    protected Integer getIntParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    protected Integer getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Integer value = getIntParam(params, key);
        return value != null ? value : defaultValue;
    }

    protected Double getDoubleParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    protected Boolean getBoolParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value) || "1".equals((String) value);
        }
        return null;
    }

    protected Boolean getBoolParam(Map<String, Object> params, String key, boolean defaultValue) {
        Boolean value = getBoolParam(params, key);
        return value != null ? value : defaultValue;
    }
}
