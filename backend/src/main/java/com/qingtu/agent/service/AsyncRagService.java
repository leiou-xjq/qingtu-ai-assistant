package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.AsyncTask;
import com.qingtu.agent.mapper.AsyncTaskMapper;
import com.qingtu.agent.rag.RagServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 异步RAG服务
 * 使用 @Async 线程池异步执行 RAG 检索 + AI 生成
 * 任务状态持久化到 async_task 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncRagService {

    private final AsyncTaskMapper asyncTaskMapper;
    private final RagServiceCore ragServiceCore;

    /**
     * 创建异步RAG任务
     * 立即返回 taskId，后台异步执行检索和生成
     */
    @Transactional
    public CommonResult<?> createTask(Long userId, String question, Long sessionId) {
        AsyncTask task = new AsyncTask();
        task.setUserId(userId);
        task.setQuestion(question);
        task.setSessionId(sessionId != null ? sessionId.toString() : null);
        task.setStatus("PENDING");

        asyncTaskMapper.insert(task);

        this.executeAsyncTask(task.getId(), question);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId().toString());
        result.put("status", "PENDING");
        return CommonResult.success(result);
    }

    @Transactional
    public CommonResult<?> createSkillTask(Long userId, String question, Long sessionId) {
        return createTask(userId, question, sessionId);
    }

    /**
     * 查询任务状态
     */
    public CommonResult<?> getTaskStatus(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "NOT_FOUND");
            return CommonResult.success(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId().toString());
        result.put("status", task.getStatus());
        return CommonResult.success(result);
    }

    /**
     * 获取任务结果
     */
    public CommonResult<?> getTaskResult(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", "NOT_FOUND");
            result.put("result", "任务不存在");
            return CommonResult.success(result);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId().toString());
        result.put("status", task.getStatus());

        if ("COMPLETED".equals(task.getStatus())) {
            result.put("result", task.getAnswer());
        } else if ("FAILED".equals(task.getStatus())) {
            result.put("result", task.getErrorMessage());
        } else if ("PROCESSING".equals(task.getStatus())) {
            result.put("result", "任务处理中...");
        } else {
            result.put("result", "任务等待处理中...");
        }

        return CommonResult.success(result);
    }

    /**
     * 异步执行 RAG 任务
     * 检索知识库 → 拼接 Prompt → 返回上下文字符串
     * 注意：不在此处调用 AI（异步任务可能持续较久，AI 调用由调用方负责）
     */
    @Async
    @Transactional
    public void executeAsyncTask(Long taskId, String question) {
        try {
            AsyncTask task = asyncTaskMapper.selectById(taskId);
            if (task == null) {
                return;
            }

            task.setStatus("PROCESSING");
            asyncTaskMapper.updateById(task);

            String context = ragServiceCore.retrieveCommonContext(question, 5);

            task.setAnswer(context);
            task.setStatus("COMPLETED");
            asyncTaskMapper.updateById(task);

            log.info("异步RAG任务完成: taskId={}, contextLength={}", taskId,
                context != null ? context.length() : 0);

        } catch (Exception e) {
            log.error("异步RAG任务失败: taskId={}, error={}", taskId, e.getMessage());

            AsyncTask task = asyncTaskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                asyncTaskMapper.updateById(task);
            }
        }
    }
}
