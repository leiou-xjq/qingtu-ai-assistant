package com.qingtu.agent.tool.rag;

import com.qingtu.agent.rag.ElasticsearchRagStore;
import com.qingtu.agent.rag.dto.KnowledgeDTO;
import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RAG 文档入库工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagUpsertTool implements ToolExecutor {

    private final ElasticsearchRagStore esRagStore;

    @Override
    public String getName() {
        return "rag_upsert";
    }

    @Override
    public String getDescription() {
        return "向知识库写入或更新文档，支持按学校隔离，支持来源 URL 和标签标注";
    }

    @Override
    public String getCategory() {
        return "rag";
    }

    @Override
    public ToolDefinition.ExecuteResult execute(Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        try {
            String title = (String) arguments.get("title");
            String content = (String) arguments.get("content");
            String school = (String) arguments.getOrDefault("school", "");
            String category = (String) arguments.getOrDefault("category", "general");
            String source = (String) arguments.getOrDefault("source_url", "");
            String tags = (String) arguments.getOrDefault("tags", "");

            if (title == null || title.isBlank()) {
                return ToolDefinition.ExecuteResult.error("title 参数不能为空");
            }
            if (content == null || content.isBlank()) {
                return ToolDefinition.ExecuteResult.error("content 参数不能为空");
            }

            KnowledgeDTO dto = new KnowledgeDTO();
            dto.setTitle(title);
            dto.setContent(content);
            dto.setSchool(school);
            dto.setCategory(category);
            dto.setSource(source);
            dto.setTags(tags);

            esRagStore.addDocument(dto);

            log.info("RAG 文档入库成功: title={}, school={}", title, school);

            return ToolDefinition.ExecuteResult.success(Map.of(
                    "status", "indexed",
                    "title", title,
                    "school", school,
                    "timestamp", java.time.LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            log.error("RAG 文档入库失败", e);
            return ToolDefinition.ExecuteResult.error("RAG 文档入库失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeoutMs() {
        return 20000;
    }
}
