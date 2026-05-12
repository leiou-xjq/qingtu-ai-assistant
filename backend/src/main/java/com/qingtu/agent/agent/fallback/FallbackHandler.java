package com.qingtu.agent.agent.fallback;

import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 降级处理器
 * 当 Agent 执行失败并重试 3 次后，执行降级策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FallbackHandler {

    private static final int MAX_RETRIES = 3;

    public boolean shouldFallback(int retryCount) {
        return retryCount >= MAX_RETRIES;
    }

    public ResultMessage weatherFallback(String taskId, String action, String correlationId, UserContext context) {
        log.warn("天气查询降级: taskId={}", taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "fallback");
        result.put("message", "天气服务暂时不可用，建议查看手机天气APP获取最新信息");
        result.put("city", context != null ? context.getCity() : "未知");

        return ResultMessage.fallback(taskId, "weather", action, result, correlationId, context != null ? context.getUserId() : null);
    }

    public ResultMessage expenseFallback(String taskId, String action, String correlationId, UserContext context, Map<String, Object> params) {
        log.warn("记账降级: taskId={}, params={}", taskId, params);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "fallback");
        result.put("message", "记账服务暂时不可用，以下是模拟数据（实际未保存）：");

        Double amount = params != null && params.containsKey("amount")
                ? ((Number) params.get("amount")).doubleValue() : 0;
        String category = params != null && params.containsKey("category")
                ? (String) params.get("category") : "其他";

        result.put("mockExpense", Map.of(
                "amount", amount,
                "category", category,
                "note", "（模拟数据，记账失败）"
        ));

        return ResultMessage.fallback(taskId, "expense", action, result, correlationId, context != null ? context.getUserId() : null);
    }

    public ResultMessage courseFallback(String taskId, String action, String correlationId, UserContext context, Map<String, Object> params) {
        log.warn("课程导入降级: taskId={}", taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "fallback");
        result.put("message", "课程导入服务暂时不可用，建议稍后重试或手动导入");
        result.put("fileName", params != null ? params.get("fileName") : "未知文件");

        return ResultMessage.fallback(taskId, "course", action, result, correlationId, context != null ? context.getUserId() : null);
    }

    public ResultMessage profileFallback(String taskId, String action, String correlationId, UserContext context, Map<String, Object> params) {
        log.warn("资料修改降级: taskId={}", taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "fallback");
        result.put("message", "资料修改服务暂时不可用，请稍后重试或在个人资料页面手动修改");

        StringBuilder fields = new StringBuilder();
        if (params != null) {
            params.forEach((key, value) -> {
                if (fields.length() > 0) fields.append(", ");
                fields.append(key).append("=").append(value);
            });
        }
        result.put("attemptedFields", fields.toString());

        return ResultMessage.fallback(taskId, "profile", action, result, correlationId, context != null ? context.getUserId() : null);
    }

    public ResultMessage noteFallback(String taskId, String action, String correlationId, UserContext context, Map<String, Object> params) {
        log.warn("笔记生成降级: taskId={}", taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "fallback");
        result.put("message", "笔记生成服务暂时不可用，您也可以先记录要点，稍后我会帮您整理");
        result.put("courseName", params != null ? params.get("courseName") : "未知课程");

        return ResultMessage.fallback(taskId, "note", action, result, correlationId, context != null ? context.getUserId() : null);
    }

    public ResultMessage handleFallback(String agent, String taskId, String action, String correlationId, UserContext context, Map<String, Object> params) {
        return switch (agent.toLowerCase()) {
            case "weather" -> weatherFallback(taskId, action, correlationId, context);
            case "expense" -> expenseFallback(taskId, action, correlationId, context, params);
            case "course" -> courseFallback(taskId, action, correlationId, context, params);
            case "profile" -> profileFallback(taskId, action, correlationId, context, params);
            case "note" -> noteFallback(taskId, action, correlationId, context, params);
            default -> {
                log.warn("未知Agent类型降级: {}", agent);
                Map<String, Object> result = new HashMap<>();
                result.put("status", "fallback");
                result.put("message", "服务暂时不可用，请稍后重试");
                yield ResultMessage.fallback(taskId, agent, action, result, correlationId, context != null ? context.getUserId() : null);
            }
        };
    }
}