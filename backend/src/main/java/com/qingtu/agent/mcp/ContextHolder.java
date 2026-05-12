package com.qingtu.agent.mcp;

import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP统一上下文持有者
 * 
 * 功能说明：
 * - 在MCP调度过程中传递统一上下文
 * - 包含用户信息、任务参数、结果等
 * - 支持上下文继承和覆盖
 * 
 * @author 青途智伴技术团队
 */
@Data
@Accessors(chain = true)
public class ContextHolder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 城市（用于天气）
     */
    private String city;

    /**
     * 当前任务标识
     */
    private String taskKey;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 执行结果
     */
    private Object result;

    /**
     * 执行状态
     */
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private long startTime;

    /**
     * 结束时间
     */
    private long endTime;

    /**
     * 执行耗时（毫秒）
     */
    private long duration;

    /**
     * 创建上下文
     */
    public static ContextHolder create() {
        return new ContextHolder();
    }

    /**
     * 创建带用户ID的上下文
     */
    public static ContextHolder create(Long userId) {
        return new ContextHolder().setUserId(userId);
    }

    /**
     * 设置参数
     */
    public ContextHolder putParam(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * 获取参数
     */
    public <T> T getParam(String key) {
        return (T) params.get(key);
    }

    /**
     * 获取参数（带默认值）
     */
    public <T> T getParam(String key, T defaultValue) {
        Object value = params.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 标记开始
     */
    public void markStart() {
        this.startTime = System.currentTimeMillis();
        this.status = TaskStatus.RUNNING;
    }

    /**
     * 标记成功
     */
    public void markSuccess(Object result) {
        this.endTime = System.currentTimeMillis();
        this.duration = this.endTime - this.startTime;
        this.result = result;
        this.status = TaskStatus.SUCCESS;
    }

    /**
     * 标记失败
     */
    public void markFail(String errorMessage) {
        this.endTime = System.currentTimeMillis();
        this.duration = this.endTime - this.startTime;
        this.errorMessage = errorMessage;
        this.status = TaskStatus.FAILED;
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 待执行
        RUNNING,    // 执行中
        SUCCESS,    // 成功
        FAILED,     // 失败
        CANCELLED   // 已取消
    }
}