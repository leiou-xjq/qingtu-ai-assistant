package com.qingtu.agent.agent.orchestrator;

import lombok.Data;

import java.util.Map;

/**
 * Agent 执行任务
 */
@Data
public class Task {

    private String taskId;
    private String agent;
    private String action;
    private Map<String, Object> parameters;
    private String description;
    private String correlationId;

    public static Task of(String agent, String action, Map<String, Object> parameters) {
        Task task = new Task();
        task.setTaskId(java.util.UUID.randomUUID().toString());
        task.setAgent(agent);
        task.setAction(action);
        task.setParameters(parameters);
        return task;
    }

    public static Task of(String agent, String action, Map<String, Object> parameters, String description) {
        Task task = of(agent, action, parameters);
        task.setDescription(description);
        return task;
    }
}