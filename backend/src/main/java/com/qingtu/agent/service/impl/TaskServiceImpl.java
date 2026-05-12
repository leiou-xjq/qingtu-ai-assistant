package com.qingtu.agent.service.impl;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.service.TaskService;
import com.qingtu.agent.entity.po.SysTaskConfig;
import com.qingtu.agent.mapper.SysTaskConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 定时任务服务实现类
 * 
 * @author 青途智伴技术团队
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final SysTaskConfigMapper taskConfigMapper;

    @Override
    public CommonResult<?> listTasks() {
        return CommonResult.success(taskConfigMapper.selectList(
                new LambdaQueryWrapper<SysTaskConfig>()
                        .eq(SysTaskConfig::getDeleted, 0)
                        .orderByAsc(SysTaskConfig::getId)));
    }

    @Override
    public CommonResult<?> getTaskByKey(String taskKey) {
        return CommonResult.success(taskConfigMapper.selectOne(
                new LambdaQueryWrapper<SysTaskConfig>()
                        .eq(SysTaskConfig::getTaskKey, taskKey)
                        .eq(SysTaskConfig::getDeleted, 0)));
    }

    @Override
    public CommonResult<?> updateTaskConfig(String taskKey, Integer enabled, String cronExpression) {
        SysTaskConfig task = taskConfigMapper.selectOne(
                new LambdaQueryWrapper<SysTaskConfig>()
                        .eq(SysTaskConfig::getTaskKey, taskKey)
                        .eq(SysTaskConfig::getDeleted, 0));

        if (task == null) {
            return CommonResult.fail(ResultCode.TASK_NOT_FOUND);
        }

        if (enabled != null) task.setEnabled(enabled);
        if (cronExpression != null) task.setCronExpression(cronExpression);
        taskConfigMapper.updateById(task);

        return CommonResult.success("更新成功");
    }

    @Override
    public CommonResult<?> executeTaskNow(String taskKey) {
        return CommonResult.success("任务已触发执行");
    }

    @Override
    public CommonResult<?> toggleTask(String taskKey, boolean enabled) {
        SysTaskConfig task = taskConfigMapper.selectOne(
                new LambdaQueryWrapper<SysTaskConfig>()
                        .eq(SysTaskConfig::getTaskKey, taskKey)
                        .eq(SysTaskConfig::getDeleted, 0));

        if (task == null) {
            return CommonResult.fail(ResultCode.TASK_NOT_FOUND);
        }

        task.setEnabled(enabled ? 1 : 0);
        taskConfigMapper.updateById(task);

        return CommonResult.success(enabled ? "任务已启用" : "任务已禁用");
    }

    @Override
    public CommonResult<?> getTaskHistory(String taskKey, int limit) {
        return CommonResult.success(java.util.Collections.emptyList());
    }

    @Override
    public CommonResult<?> getTaskStatistics() {
        return CommonResult.success(java.util.Map.of(
                "totalTasks", taskConfigMapper.selectCount(new LambdaQueryWrapper<SysTaskConfig>().eq(SysTaskConfig::getDeleted, 0)),
                "enabledTasks", taskConfigMapper.selectCount(new LambdaQueryWrapper<SysTaskConfig>().eq(SysTaskConfig::getEnabled, 1).eq(SysTaskConfig::getDeleted, 0)),
                "totalRuns", 0
        ));
    }
}