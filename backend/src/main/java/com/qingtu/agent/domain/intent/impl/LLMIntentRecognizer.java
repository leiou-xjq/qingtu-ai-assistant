package com.qingtu.agent.domain.intent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.domain.intent.IntentRecognizer;
import com.qingtu.agent.domain.intent.RecognitionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LLMIntentRecognizer implements IntentRecognizer {

    private final DashScopeConfig dashScopeConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INTENT_WEATHER = "weather";
    private static final String INTENT_EXPENSE = "expense";
    private static final String INTENT_COURSE = "course";
    private static final String INTENT_PROFILE = "profile";
    private static final String INTENT_NOTE = "note";
    private static final String INTENT_CALORIE = "calorie";
    private static final String INTENT_CHAT = "chat";
    private static final String INTENT_SEARCH = "search";

    @Override
    public RecognitionResult recognize(String message, UserContext context) {
        List<RecognitionResult> candidates = recognizeWithCandidates(message, context);
        if (candidates.isEmpty()) {
            return RecognitionResult.of(INTENT_CHAT, 1.0, "chat", Map.of("message", message));
        }
        RecognitionResult top = candidates.get(0);
        if (top.getConfidence() < MIN_CONFIDENCE) {
            log.debug("Intent {} confidence {} below threshold {}, falling back to CHAT",
                top.getIntent(), top.getConfidence(), MIN_CONFIDENCE);
            return RecognitionResult.of(INTENT_CHAT, 1.0, "chat", Map.of("message", message));
        }
        return top;
    }

    @Override
    public List<RecognitionResult> recognizeWithCandidates(String message, UserContext context) {
        try {
            String prompt = buildPrompt(message, context, null);
            String llmResponse = callLLM(prompt);
            return parseResponse(llmResponse, context, message);
        } catch (Exception e) {
            log.error("Intent recognition failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildPrompt(String message, UserContext context, Map<String, String> files) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是青途AI助手的意图分析器。分析用户消息，识别用户想要执行的任务。\n\n");

        prompt.append("【可用Agent类型】\n");
        prompt.append("- weather: 天气查询\n");
        prompt.append("- expense: 记账创建\n");
        prompt.append("- course: 课程导入\n");
        prompt.append("- profile: 个人信息修改\n");
        prompt.append("- note: 笔记生成\n");
        prompt.append("- calorie: 卡路里记录\n");
        prompt.append("- search: 联网搜索\n");
        prompt.append("- chat: 普通对话\n\n");

        prompt.append("【用户上下文】\n");
        prompt.append("- 用户ID: ").append(context.getUserId()).append("\n");
        prompt.append("- 城市: ").append(context.getCity() != null ? context.getCity() : "未知").append("\n");
        prompt.append("- 学校: ").append(context.getSchool() != null ? context.getSchool() : "未知").append("\n\n");

        prompt.append("【用户消息】\n").append(message).append("\n\n");

        prompt.append("【输出要求】输出JSON格式：{\"tasks\": [{\"agent\": \"类型\", \"action\": \"动作\", \"params\": {}, \"confidence\": 0.9}]}\n");
        return prompt.toString();
    }

    private String callLLM(String prompt) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
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
            log.error("LLM response parsing failed", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<RecognitionResult> parseResponse(String llmResponse, UserContext context, String originalMessage) {
        List<RecognitionResult> results = new ArrayList<>();

        if (llmResponse == null || llmResponse.isBlank()) {
            return results;
        }

        try {
            int startIdx = llmResponse.indexOf('{');
            int endIdx = llmResponse.lastIndexOf('}');
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonStr = llmResponse.substring(startIdx, endIdx + 1);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);

                List<Map<String, Object>> taskList = (List<Map<String, Object>>) parsed.getOrDefault("tasks", new ArrayList<>());
                for (Map<String, Object> taskMap : taskList) {
                    String agent = (String) taskMap.getOrDefault("agent", INTENT_CHAT);
                    String action = (String) taskMap.getOrDefault("action", "chat");
                    Map<String, Object> params = (Map<String, Object>) taskMap.getOrDefault("params", new HashMap<>());
                    double confidence = 1.0;
                    Object confidenceObj = taskMap.get("confidence");
                    if (confidenceObj instanceof Number) {
                        confidence = ((Number) confidenceObj).doubleValue();
                    }

                    if (context != null) {
                        params.put("_userId", context.getUserId());
                        params.put("_city", context.getCity());
                        params.put("_school", context.getSchool());
                    }

                    if (confidence >= MIN_CONFIDENCE) {
                        results.add(RecognitionResult.of(agent, confidence, action, params));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Parsing intent response failed: {}", e.getMessage());
        }

        return results;
    }
}