package com.qingtu.agent.mcp.server;

import com.qingtu.agent.mcp.server.audit.McpAuditService;
import com.qingtu.agent.mcp.server.auth.ToolPermission;
import com.qingtu.agent.mcp.server.config.McpServerConfig;
import com.qingtu.agent.mcp.server.tools.DocTools;
import com.qingtu.agent.mcp.server.tools.RagTools;
import com.qingtu.agent.mcp.server.tools.SchoolTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP Server 核心调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QingTuMcpServer {

    private final McpServerConfig config;
    private final McpAuditService auditService;
    private final SchoolTools schoolTools;
    private final RagTools ragTools;
    private final DocTools docTools;

    private final Map<String, ToolHandlerData> toolHandlers = new HashMap<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        // school tools
        toolHandlers.put("school_search", new ToolHandlerData("school_search", p -> {
            return schoolTools.searchSchool((String) p.get("keyword"));
        }, ToolPermission.USER, "school"));

        toolHandlers.put("school_info", new ToolHandlerData("school_info", p -> {
            Long userId = p.get("userId") != null ? ((Number) p.get("userId")).longValue() : null;
            return schoolTools.getSchoolInfo(userId, (String) p.get("schoolName"));
        }, ToolPermission.USER, "school"));

        toolHandlers.put("school_knowledge_upsert", new ToolHandlerData("school_knowledge_upsert", p -> {
            return schoolTools.upsertSchoolKnowledge(
                    (String) p.get("school"), (String) p.get("title"),
                    (String) p.get("content"), (String) p.get("sourceUrl"),
                    (String) p.get("category"));
        }, ToolPermission.ADMIN, "school"));

        toolHandlers.put("school_knowledge_count", new ToolHandlerData("school_knowledge_count", p -> {
            return schoolTools.getSchoolKnowledgeCount((String) p.get("school"));
        }, ToolPermission.USER, "school"));

        // rag tools
        toolHandlers.put("rag_search", new ToolHandlerData("rag_search", p -> {
            return ragTools.searchKnowledge(
                    (String) p.get("query"), (String) p.get("schoolId"),
                    p.get("topK") != null ? ((Number) p.get("topK")).intValue() : 5);
        }, ToolPermission.USER, "rag"));

        toolHandlers.put("rag_batch_upsert", new ToolHandlerData("rag_batch_upsert", p -> {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> docs = (List<Map<String, String>>) p.get("documents");
            return ragTools.batchUpsert(docs);
        }, ToolPermission.ADMIN, "rag"));

        toolHandlers.put("rag_clear", new ToolHandlerData("rag_clear", p -> {
            return ragTools.clearKnowledgeBase();
        }, ToolPermission.ADMIN, "rag"));

        toolHandlers.put("rag_stats", new ToolHandlerData("rag_stats", p -> {
            return ragTools.getKnowledgeStats();
        }, ToolPermission.USER, "rag"));

        // doc tools
        toolHandlers.put("parse_document", new ToolHandlerData("parse_document", p -> {
            return docTools.parseDocument((String) p.get("fileUrl"), (String) p.get("fileType"));
        }, ToolPermission.USER, "doc"));

        toolHandlers.put("ocr_image", new ToolHandlerData("ocr_image", p -> {
            return docTools.ocrImage((String) p.get("imageUrl"));
        }, ToolPermission.USER, "doc"));

        toolHandlers.put("extract_schedule", new ToolHandlerData("extract_schedule", p -> {
            return docTools.extractSchedule(
                    (String) p.get("rawText"), (String) p.get("schoolName"),
                    (String) p.get("semesterStart"));
        }, ToolPermission.USER, "doc"));

        log.info("MCP Server 初始化完成，共注册 {} 个工具", toolHandlers.size());
    }

    public McpResponse execute(McpRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString();

        try {
            if (config.getSecurity().isEnablePermissionCheck()) {
                ToolHandlerData handler = toolHandlers.get(request.getMethod());
                if (handler == null) {
                    return McpResponse.error(requestId, "TOOL_NOT_FOUND", "工具不存在: " + request.getMethod(),
                            System.currentTimeMillis() - startTime);
                }
                if (!hasPermission(request.getUserId(), handler.permission)) {
                    return McpResponse.error(requestId, "PERMISSION_DENIED", "权限不足",
                            System.currentTimeMillis() - startTime);
                }
            }

            ToolHandlerData handler = toolHandlers.get(request.getMethod());
            Object result = handler.execute(request.getParams());

            if (config.getAudit().isEnabled() && config.getAudit().isLogToolCalls()) {
                String ip = request.getHeaders() != null ? request.getHeaders().get("X-Forwarded-For") : null;
                auditService.logToolCall(
                        request.getMethod(), handler.category, request.getUserId(),
                        request.getParams(), result, "SUCCESS",
                        System.currentTimeMillis() - startTime, ip, null);
            }

            return McpResponse.success(requestId, result, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("MCP 工具执行异常: method={}", request.getMethod(), e);
            ToolHandlerData handler = toolHandlers.get(request.getMethod());
            if (config.getAudit().isEnabled()) {
                String ip = request.getHeaders() != null ? request.getHeaders().get("X-Forwarded-For") : null;
                auditService.logToolCall(
                        request.getMethod(), handler != null ? handler.category : "unknown",
                        request.getUserId(), request.getParams(), null, "FAILED",
                        System.currentTimeMillis() - startTime, ip, e.getMessage());
            }
            return McpResponse.error(requestId, "EXECUTION_ERROR", e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    public List<Map<String, Object>> getAvailableTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolHandlerData handler : toolHandlers.values()) {
            tools.add(Map.of("name", handler.toolName, "category", handler.category,
                    "permission", handler.permission.name()));
        }
        return tools;
    }

public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        Map<String, Object> keywordProp = new HashMap<>();
        keywordProp.put("type", "string");
        keywordProp.put("description", "搜索关键词");
        Map<String, Object> schoolSearchParams = new HashMap<>();
        schoolSearchParams.put("type", "object");
        schoolSearchParams.put("properties", Map.of("keyword", keywordProp));
        Map<String, Object> schoolSearchFunc = new HashMap<>();
        schoolSearchFunc.put("name", "school_search");
        schoolSearchFunc.put("description", "搜索学校信息");
        schoolSearchFunc.put("parameters", schoolSearchParams);
        Map<String, Object> schoolSearch = new HashMap<>();
        schoolSearch.put("type", "function");
        schoolSearch.put("function", schoolSearchFunc);
        tools.add(schoolSearch);

        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "搜索查询");
        Map<String, Object> ragSearchParams = new HashMap<>();
        ragSearchParams.put("type", "object");
        ragSearchParams.put("properties", Map.of("query", queryProp));
        Map<String, Object> ragSearchFunc = new HashMap<>();
        ragSearchFunc.put("name", "rag_search");
        ragSearchFunc.put("description", "在知识库中检索相关内容");
        ragSearchFunc.put("parameters", ragSearchParams);
        Map<String, Object> ragSearch = new HashMap<>();
        ragSearch.put("type", "function");
        ragSearch.put("function", ragSearchFunc);
        tools.add(ragSearch);

        Map<String, Object> fileUrlProp = new HashMap<>();
        fileUrlProp.put("type", "string");
        fileUrlProp.put("description", "文件URL");
        Map<String, Object> parseDocParams = new HashMap<>();
        parseDocParams.put("type", "object");
        parseDocParams.put("properties", Map.of("fileUrl", fileUrlProp));
        Map<String, Object> parseDocFunc = new HashMap<>();
        parseDocFunc.put("name", "parse_document");
        parseDocFunc.put("description", "解析文档");
        parseDocFunc.put("parameters", parseDocParams);
        Map<String, Object> parseDoc = new HashMap<>();
        parseDoc.put("type", "function");
        parseDoc.put("function", parseDocFunc);
        tools.add(parseDoc);

        Map<String, Object> imageUrlProp = new HashMap<>();
        imageUrlProp.put("type", "string");
        imageUrlProp.put("description", "图片URL");
        Map<String, Object> ocrParams = new HashMap<>();
        ocrParams.put("type", "object");
        ocrParams.put("properties", Map.of("imageUrl", imageUrlProp));
        Map<String, Object> ocrFunc = new HashMap<>();
        ocrFunc.put("name", "ocr_image");
        ocrFunc.put("description", "图片OCR文字识别");
        ocrFunc.put("parameters", ocrParams);
        Map<String, Object> ocr = new HashMap<>();
        ocr.put("type", "function");
        ocr.put("function", ocrFunc);
        tools.add(ocr);

        Map<String, Object> rawTextProp = new HashMap<>();
        rawTextProp.put("type", "string");
        rawTextProp.put("description", "原始课表文本");
        Map<String, Object> extractParams = new HashMap<>();
        extractParams.put("type", "object");
        extractParams.put("properties", Map.of("rawText", rawTextProp));
        Map<String, Object> extractFunc = new HashMap<>();
        extractFunc.put("name", "extract_schedule");
        extractFunc.put("description", "从课表文本中提取结构化课程信息");
        extractFunc.put("parameters", extractParams);
        Map<String, Object> extractSchedule = new HashMap<>();
        extractSchedule.put("type", "function");
        extractSchedule.put("function", extractFunc);
        tools.add(extractSchedule);

        return tools;
    }

    private boolean hasPermission(Long userId, ToolPermission required) {
        return required == ToolPermission.USER || required == ToolPermission.PUBLIC;
    }

    private static class ToolHandlerData {
        final String toolName;
        final ToolExecutor executor;
        final ToolPermission permission;
        final String category;

        ToolHandlerData(String toolName, ToolExecutor executor, ToolPermission permission, String category) {
            this.toolName = toolName;
            this.executor = executor;
            this.permission = permission;
            this.category = category;
        }

        Object execute(Map<String, Object> params) {
            return executor.execute(params);
        }
    }

    @FunctionalInterface
    interface ToolExecutor {
        Map<String, Object> execute(Map<String, Object> params);
    }
}
