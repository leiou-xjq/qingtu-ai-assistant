package com.qingtu.agent.agent.message;

import lombok.Data;

import java.io.Serializable;

/**
 * Agent 执行结果消息
 * 从各 Specialist Agent 返回给 Orchestrator
 */
@Data
public class ResultMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String agent;
    private String action;
    private boolean success;
    private Object result;
    private String errorMessage;
    private boolean fallback;
    private long executionTimeMs;
    private String correlationId;
    private Long userId;

    public static ResultMessage success(String taskId, String agent, String action, Object result, String correlationId, Long userId) {
        ResultMessage message = new ResultMessage();
        message.setTaskId(taskId);
        message.setAgent(agent);
        message.setAction(action);
        message.setSuccess(true);
        message.setResult(result);
        message.setCorrelationId(correlationId);
        message.setUserId(userId);
        return message;
    }

    public static ResultMessage failure(String taskId, String agent, String action, String errorMessage, String correlationId, Long userId) {
        ResultMessage message = new ResultMessage();
        message.setTaskId(taskId);
        message.setAgent(agent);
        message.setAction(action);
        message.setSuccess(false);
        message.setErrorMessage(errorMessage);
        message.setCorrelationId(correlationId);
        message.setUserId(userId);
        return message;
    }

    public static ResultMessage fallback(String taskId, String agent, String action, Object result, String correlationId, Long userId) {
        ResultMessage message = success(taskId, agent, action, result, correlationId, userId);
        message.setFallback(true);
        return message;
    }

    public void setExecutionTime(long startTime) {
        this.executionTimeMs = System.currentTimeMillis() - startTime;
    }
}