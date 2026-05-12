package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

/**
 * RAG知识库服务接口
 *
 * 定义RAG检索相关的业务操作
 *
 * @author 青途智伴技术团队
 */
public interface IRagService {

    CommonResult<?> ask(Long userId, String question, Long sessionId);

    CommonResult<?> search(String query, String category, int topK);

    CommonResult<?> getSessionList(Long userId);

    CommonResult<?> getSessionHistory(Long sessionId);

    CommonResult<?> createSession(Long userId, String title);

    CommonResult<?> deleteSession(Long sessionId);

    CommonResult<?> renameSession(Long sessionId, String newTitle);
}