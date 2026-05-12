package com.qingtu.agent.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.rag.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 联网搜索Agent
 * 使用LLM的联网搜索能力获取实时信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSpecialistAgent {

    private final QingTuAgent qingTuAgent;
    private final DashScopeConfig dashScopeConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        String taskId = UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();
        String query = params.getOrDefault("query", params.getOrDefault("message", "")).toString();

        try {
            String result = webSearch(query, context);
            return ResultMessage.success(taskId, "search", action, result, correlationId, context.getUserId());
        } catch (Exception e) {
            log.error("联网搜索失败", e);
            return ResultMessage.failure(taskId, "search", action, "搜索失败：" + e.getMessage(), correlationId, context.getUserId());
        }
    }

    public String webSearch(String query, UserContext context) {
        log.info("联网搜索: query={}", query);

        String prompt = PromptBuilder.create()
                .system("你是青途智伴AI助手，专门为大学生提供校园生活服务")
                .question(query)
                .instruction("请搜索相关信息并给出准确回答", "如果搜索不到相关信息，请如实告知", "用中文简洁回答", "优先提供最新信息")
                .build();

        return callSearchLLM(prompt);
    }

    private String callSearchLLM(String prompt) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("enable_search", true);
        body.put("temperature", 0.3);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(url, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            log.error("解析搜索响应失败", e);
        }
        return "搜索服务暂时不可用，请稍后重试";
    }

    public Flux<String> webSearchStream(String query, UserContext context) {
        log.info("联网搜索(流式): query={}", query);

        String prompt = PromptBuilder.create()
                .system("你是青途智伴AI助手，专门为大学生提供校园生活服务")
                .question(query)
                .instruction("请搜索相关信息并给出准确回答", "用中文简洁回答")
                .build();

        return qingTuAgent.chatStream(prompt);
    }
}
