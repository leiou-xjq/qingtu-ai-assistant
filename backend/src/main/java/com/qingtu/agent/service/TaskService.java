package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.SysTaskConfig;
import java.util.List;

/**
 * 定时任务服务接口
 * 
 * 定义定时任务相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface TaskService {

    /**
     * 获取所有任务配置
     * 
     * @return 任务配置列表
     */
    CommonResult<?> listTasks();

    /**
     * 获取任务配置详情
     * 
     * @param taskKey 任务标识
     * @return 任务配置
     */
    CommonResult<?> getTaskByKey(String taskKey);

    /**
     * 更新任务配置
     * 
     * @param taskKey 任务标识
     * @param enabled 是否启用
     * @param cronExpression Cron表达式
     * @return 更新结果
     */
    CommonResult<?> updateTaskConfig(String taskKey, Integer enabled, String cronExpression);

    /**
     * 立即执行任务
     * 
     * @param taskKey 任务标识
     * @return 执行结果
     */
    CommonResult<?> executeTaskNow(String taskKey);

    /**
     * 启用/禁用任务
     * 
     * @param taskKey 任务标识
     * @param enabled 是否启用
     * @return 操作结果
     */
    CommonResult<?> toggleTask(String taskKey, boolean enabled);

    /**
     * 获取任务执行历史
     * 
     * @param taskKey 任务标识
     * @param limit 返回数量
     * @return 执行历史
     */
    CommonResult<?> getTaskHistory(String taskKey, int limit);

    /**
     * 获取任务统计信息
     * 
     * @return 统计信息
     */
    CommonResult<?> getTaskStatistics();
}