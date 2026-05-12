package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 异步RAG服务
 * 提供异步任务创建和状态查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncRagService {

    private final UserMapper userMapper;

    public CommonResult<?> createTask(Long userId, String question, Long sessionId) {
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", "created");
        return CommonResult.success(result);
    }

    public CommonResult<?> createSkillTask(Long userId, String question, Long sessionId) {
        return createTask(userId, question, sessionId);
    }

    public CommonResult<?> getTaskStatus(Long taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", "completed");
        return CommonResult.success(result);
    }

    public CommonResult<?> getTaskResult(Long taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", "completed");
        result.put("result", "任务已完成");
        return CommonResult.success(result);
    }
}
