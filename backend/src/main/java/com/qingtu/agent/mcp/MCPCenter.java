package com.qingtu.agent.mcp;

import com.qingtu.agent.rag.RagServiceCore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP多组件协同调度中心
 * Skill机制已废弃，定时任务直接调用底层Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MCPCenter {

    private final RagServiceCore ragService;

    public ContextHolder executeMorningPush(ContextHolder context) {
        context.setTaskKey("morning_push");
        log.info("【MCP】早安推送（Skill已废弃）");
        context.markSuccess("ok");
        return context;
    }

    public ContextHolder executeCourseNote(ContextHolder context) {
        context.setTaskKey("course_note");
        log.info("【MCP】课程笔记生成（Skill已废弃）");
        context.markSuccess("ok");
        return context;
    }

    public ContextHolder executeCostReport(ContextHolder context) {
        context.setTaskKey("monthly_report");
        log.info("【MCP】月度消费报告（Skill已废弃）");
        context.markSuccess("ok");
        return context;
    }

    public String executeRAGQuery(String query, Long userId) {
        try {
            return ragService.retrieveCommonContext(query, 5);
        } catch (Exception e) {
            log.error("【MCP】RAG检索异常", e);
            return null;
        }
    }
}