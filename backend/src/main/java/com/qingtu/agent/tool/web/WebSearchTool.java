package com.qingtu.agent.tool.web;

import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 联网搜索工具
 * 调用 DashScope 内置联网搜索能力
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchTool implements ToolExecutor {

    private final RestTemplate restTemplate;

    @Override
    public String getName() {
        return "web_search_fetch";
    }

    @Override
    public String getDescription() {
        return "联网搜索获取最新信息，返回搜索结果列表包含标题、URL、摘要和发布时间";
    }

    @Override
    public String getCategory() {
        return "web";
    }

    @Override
    public ToolDefinition.ExecuteResult execute(Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        try {
            String query = (String) arguments.get("query");
            String schoolFilter = (String) arguments.getOrDefault("school_filter", "");
            int maxResults = arguments.containsKey("max_results")
                    ? ((Number) arguments.get("max_results")).intValue()
                    : 5;

            if (query == null || query.isBlank()) {
                return ToolDefinition.ExecuteResult.error("query 参数不能为空");
            }

            // 实际场景：调用联网搜索 API（如 Tavily、Bing 等）
            // 这里模拟返回
            List<Map<String, Object>> results = new ArrayList<>();

            // 如果有学校信息，增强搜索 query
            String enhancedQuery = query;
            if (schoolFilter != null && !schoolFilter.isBlank()) {
                enhancedQuery = schoolFilter + " " + query;
            }

            // TODO: 调用真实联网搜索 API
            // 模拟返回
            Map<String, Object> mockResult = new HashMap<>();
            mockResult.put("title", "关于 " + enhancedQuery + " 的搜索结果");
            mockResult.put("url", "https://example.com/search?q=" + enhancedQuery);
            mockResult.put("snippet", "这是搜索结果摘要，包含 " + enhancedQuery + " 的相关信息...");
            mockResult.put("published_date", new Date().toString());
            results.add(mockResult);

            log.info("联网搜索完成: query={}, results={}", enhancedQuery, results.size());

            return ToolDefinition.ExecuteResult.success(Map.of(
                    "query", enhancedQuery,
                    "results", results,
                    "total", results.size()
            ));

        } catch (Exception e) {
            log.error("联网搜索失败", e);
            return ToolDefinition.ExecuteResult.error("联网搜索失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeoutMs() {
        return 20000;
    }
}
