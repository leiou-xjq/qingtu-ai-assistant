package com.qingtu.agent.mcp.server.tools;

import com.qingtu.agent.mcp.server.auth.RequireToolPermission;
import com.qingtu.agent.mcp.server.auth.ToolPermission;
import com.qingtu.agent.rag.ElasticsearchRagStore;
import com.qingtu.agent.rag.RagServiceCore;
import com.qingtu.agent.rag.dto.KnowledgeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 工具集
 * 提供知识库检索、文档入库等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTools {

    private final RagServiceCore ragServiceCore;
    private final ElasticsearchRagStore esRagStore;

    /**
     * 知识库检索
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> searchKnowledge(String query, String schoolId, int topK) {
        long startTime = System.currentTimeMillis();
        try {
            RagServiceCore.RetrieveOption option = schoolId != null && !schoolId.isBlank()
                    ? RagServiceCore.RetrieveOption.schoolOnly(schoolId, topK)
                    : RagServiceCore.RetrieveOption.commonOnly(topK);

            String results = ragServiceCore.retrieve(query, option);

            return Map.of(
                    "success", true,
                    "query", query,
                    "results", results,
                    "school", schoolId != null ? schoolId : "通用知识库",
                    "topK", topK,
                    "executionTimeMs", System.currentTimeMillis() - startTime
            );
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 批量入库文档
     */
    @RequireToolPermission(ToolPermission.ADMIN)
    public Map<String, Object> batchUpsert(List<Map<String, String>> documents) {
        int successCount = 0;
        int failCount = 0;

        for (Map<String, String> doc : documents) {
            try {
                KnowledgeDTO dto = new KnowledgeDTO();
                dto.setTitle(doc.getOrDefault("title", ""));
                dto.setContent(doc.getOrDefault("content", ""));
                dto.setSchool(doc.getOrDefault("school", ""));
                dto.setCategory(doc.getOrDefault("category", "general"));
                dto.setSource(doc.getOrDefault("sourceUrl", ""));
                dto.setTags(doc.getOrDefault("tags", ""));

                esRagStore.addDocument(dto);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("文档入库失败: {}", e.getMessage());
            }
        }

        return Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "total", documents.size()
        );
    }

    /**
     * 清空知识库
     */
    @RequireToolPermission(ToolPermission.ADMIN)
    public Map<String, Object> clearKnowledgeBase() {
        try {
            esRagStore.clearAll();
            return Map.of("status", "cleared");
        } catch (Exception e) {
            return Map.of("status", "failed", "error", e.getMessage());
        }
    }

    /**
     * 获取知识库统计
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> getKnowledgeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("common", esRagStore.countCommon());
        return stats;
    }
}
