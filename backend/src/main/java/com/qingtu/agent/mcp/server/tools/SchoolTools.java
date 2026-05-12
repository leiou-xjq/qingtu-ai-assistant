package com.qingtu.agent.mcp.server.tools;

import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mcp.server.audit.McpAuditService;
import com.qingtu.agent.mcp.server.auth.RequireToolPermission;
import com.qingtu.agent.mcp.server.auth.ToolPermission;
import com.qingtu.agent.rag.ElasticsearchRagStore;
import com.qingtu.agent.rag.dto.KnowledgeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学校工具集
 * 提供学校信息查询、学校知识库操作等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchoolTools {

    private final UserMapper userMapper;
    private final ElasticsearchRagStore esRagStore;
    private final McpAuditService auditService;

    /**
     * 搜索学校信息
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> searchSchool(String keyword) {
        log.info("搜索学校: keyword={}", keyword);
        // TODO: 调用学校搜索 API
        return Map.of(
                "keyword", keyword,
                "results", List.of(),
                "total", 0
        );
    }

    /**
     * 获取学校基本信息
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> getSchoolInfo(Long userId, String schoolName) {
        User user = userMapper.selectById(userId);
        String actualSchool = schoolName != null ? schoolName : (user != null ? user.getSchool() : "");

        return Map.of(
                "school", actualSchool,
                "userId", userId,
                "found", actualSchool != null && !actualSchool.isBlank()
        );
    }

    /**
     * 导入学校知识
     */
    @RequireToolPermission(ToolPermission.ADMIN)
    public Map<String, Object> upsertSchoolKnowledge(String school, String title, String content,
                                                       String sourceUrl, String category) {
        try {
            KnowledgeDTO dto = new KnowledgeDTO();
            dto.setSchool(school);
            dto.setTitle(title);
            dto.setContent(content);
            dto.setSource(sourceUrl);
            dto.setCategory(category != null ? category : "general");

            esRagStore.addDocument(dto);

            log.info("学校知识入库成功: school={}, title={}", school, title);

            return Map.of(
                    "status", "indexed",
                    "school", school,
                    "title", title
            );
        } catch (Exception e) {
            log.error("学校知识入库失败", e);
            return Map.of(
                    "status", "failed",
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 查询学校知识库数量
     */
    @RequireToolPermission(ToolPermission.USER)
    public Map<String, Object> getSchoolKnowledgeCount(String school) {
        long count = esRagStore.countBySchool(school);
        return Map.of(
                "school", school,
                "knowledgeCount", count
        );
    }
}
