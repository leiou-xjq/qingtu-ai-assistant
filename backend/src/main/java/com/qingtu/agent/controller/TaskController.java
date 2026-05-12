package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务控制器
 * 
 * 提供任务配置管理、手动执行等功能
 * 
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 获取所有任务配置
     */
    @GetMapping("/list")
    public CommonResult<?> listTasks() {
        return taskService.listTasks();
    }

    /**
     * 获取任务配置详情
     */
    @GetMapping("/{taskKey}")
    public CommonResult<?> getTaskByKey(@PathVariable String taskKey) {
        return taskService.getTaskByKey(taskKey);
    }

    /**
     * 更新任务配置
     */
    @PutMapping("/{taskKey}")
    public CommonResult<?> updateTaskConfig(@PathVariable String taskKey,
                                            @RequestParam(required = false) Integer enabled,
                                            @RequestParam(required = false) String cronExpression) {
        return taskService.updateTaskConfig(taskKey, enabled, cronExpression);
    }

    /**
     * 立即执行任务
     */
    @PostMapping("/{taskKey}/execute")
    public CommonResult<?> executeTaskNow(@PathVariable String taskKey) {
        return taskService.executeTaskNow(taskKey);
    }

    /**
     * 启用/禁用任务
     */
    @PutMapping("/{taskKey}/toggle")
    public CommonResult<?> toggleTask(@PathVariable String taskKey, @RequestParam boolean enabled) {
        return taskService.toggleTask(taskKey, enabled);
    }

    /**
     * 获取任务执行历史
     */
    @GetMapping("/{taskKey}/history")
    public CommonResult<?> getTaskHistory(@PathVariable String taskKey,
                                           @RequestParam(defaultValue = "10") int limit) {
        return taskService.getTaskHistory(taskKey, limit);
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/statistics")
    public CommonResult<?> getTaskStatistics() {
        return taskService.getTaskStatistics();
    }
}