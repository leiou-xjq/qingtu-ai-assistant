package com.qingtu.agent.tool.rag;

import com.qingtu.agent.rag.RagServiceCore;
import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchTool implements ToolExecutor {

    private final RagServiceCore ragServiceCore;

    @Override
    public String getName() {
        return "rag_search";
    }

    @Override
    public String getDescription() {
        return "在知识库中检索相关内容，支持按学校名称过滤，返回检索结果及其来源信息";
    }

    @Override
    public String getCategory() {
        return "rag";
    }

    @Override
    public ToolDefinition.ExecuteResult execute(Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        try {
            String query = (String) arguments.get("query");
            String school = (String) arguments.getOrDefault("school", "");
            int topK = arguments.containsKey("topK")
                    ? ((Number) arguments.get("topK")).intValue()
                    : 5;

            if (query == null || query.isBlank()) {
                return ToolDefinition.ExecuteResult.error("query 参数不能为空");
            }

            RagServiceCore.RetrieveOption option;
            if (school != null && !school.isBlank()) {
                option = RagServiceCore.RetrieveOption.schoolOnly(school, topK);
            } else {
                option = RagServiceCore.RetrieveOption.commonOnly(topK);
            }

            String results = ragServiceCore.retrieve(query, option);

            return ToolDefinition.ExecuteResult.success(Map.of(
                    "query", query,
                    "results", results,
                    "school", school != null ? school : "通用知识库",
                    "topK", topK
            ));

        } catch (Exception e) {
            log.error("RAG 检索失败", e);
            return ToolDefinition.ExecuteResult.error("RAG 检索失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeoutMs() {
        return 15000;
    }
}
