package com.qingtu.agent.agent.message;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 任务消息
 * 从 Orchestrator 发送到各 Specialist Agent
 */
@Data
public class TaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String agent;
    private String action;
    private Long userId;
    private String userName;
    private String city;
    private String school;
    private String semesterStart;
    private Map<String, Object> params;
    private String correlationId;
    private int retryCount;
    private long timestamp;
    private Map<String, String> files;

    public TaskMessage() {
        this.taskId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.retryCount = 0;
    }

    public static TaskMessage of(String agent, String action, Long userId, Map<String, Object> params) {
        TaskMessage message = new TaskMessage();
        message.setAgent(agent);
        message.setAction(action);
        message.setUserId(userId);
        message.setParams(params);
        return message;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries;
    }
}