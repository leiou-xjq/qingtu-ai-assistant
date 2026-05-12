package com.qingtu.agent.agent.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.agent.agent.*;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.infrastructure.redis.RedisSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * ReAct执行器
 * 实现 Think → Action → Observe 推理循环
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReActExecutor {

    private final QingTuAgent qingTuAgent;
    private final DashScopeConfig dashScopeConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WeatherSpecialistAgent weatherAgent;
    private final ExpenseSpecialistAgent expenseAgent;
    private final CourseSpecialistAgent courseAgent;
    private final ProfileSpecialistAgent profileAgent;
    private final NoteSpecialistAgent noteAgent;
    private final CalorieSpecialistAgent calorieAgent;
    private final ChatSpecialistAgent chatAgent;
    private final SearchSpecialistAgent searchAgent;
    private final RedisSessionManager redisSessionManager;

    private static final int MAX_ITERATIONS = 5;
    private static final int MAX_HISTORY = 10;

    public String execute(String userMessage, UserContext context, String sessionId, Map<String, String> files) {
        log.info("ReAct执行: message={}", userMessage.substring(0, Math.min(50, userMessage.length())));

        String history = buildHistory(context, sessionId);
        String prompt = buildReActPrompt(userMessage, history, context);

        int iterations = 0;
        String currentState = "initial";
        Map<String, Object> toolResults = new HashMap<>();

        while (iterations < MAX_ITERATIONS) {
            iterations++;
            log.debug("ReAct迭代 {}: state={}", iterations, currentState);

            String llmResponse = callReActLLM(prompt, context);

            if (llmResponse == null || llmResponse.isBlank()) {
                return "处理失败，请稍后重试";
            }

            ReActStep step = parseReActResponse(llmResponse);
            if (step == null) {
                return llmResponse;
            }

            switch (step.actionType) {
                case "answer" -> {
                    return step.result;
                }
                case "weather" -> {
                    toolResults.put("weather", executeWeather(step.params, context));
                }
                case "expense" -> {
                    toolResults.put("expense", executeExpense(step.params, context));
                }
                case "course" -> {
                    toolResults.put("course", executeCourse(step.params, context, files));
                }
                case "profile" -> {
                    toolResults.put("profile", executeProfile(step.params, context));
                }
                case "note" -> {
                    toolResults.put("note", executeNote(step.params, context));
                }
                case "calorie" -> {
                    toolResults.put("calorie", executeCalorie(step.params, context));
                }
                case "chat" -> {
                    return executeChat(step.params, context, sessionId);
                }
                case "search" -> {
                    toolResults.put("search", executeSearch(step.params, context));
                }
                case "continue" -> {
                    prompt = buildContinuePrompt(prompt, step.result, toolResults);
                    continue;
                }
                default -> {
                    return step.result != null ? step.result : llmResponse;
                }
            }

            prompt = buildContinuePrompt(prompt, step.result, toolResults);
        }

        return "处理超时，请稍后重试";
    }

    private String buildReActPrompt(String userMessage, String history, UserContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是青途智伴AI助手，使用ReAct推理模式。\n\n");

        prompt.append("【可用工具】\n");
        prompt.append("- weather: 查询天气（如'今天天气怎么样'）\n");
        prompt.append("- expense: 记账（如'记账花了25元'）\n");
        prompt.append("- course: 课程管理（如'帮我看下课表'）\n");
        prompt.append("- profile: 个人信息（如'修改身高体重'）\n");
        prompt.append("- note: 笔记生成（如'生成课程笔记'）\n");
        prompt.append("- calorie: 卡路里记录（如'中午吃了牛肉面'）\n");
        prompt.append("- search: 联网搜索（如'搜索学校新闻'）\n");
        prompt.append("- chat: 普通对话（问候、闲聊等）\n");
        prompt.append("- answer: 直接回答（知识问答、闲聊等）\n\n");

        if (history != null && !history.isBlank()) {
            prompt.append("【对话历史】\n").append(history).append("\n");
        }

        prompt.append("【当前用户消息】\n").append(userMessage).append("\n\n");

        prompt.append("【输出格式】\n");
        prompt.append("请按照以下格式输出（JSON）：\n");
        prompt.append("{\n");
        prompt.append("  \"thought\": \"思考过程，说明为什么选择这个工具\",\n");
        prompt.append("  \"action\": \"选择的工具名称（如weather/chat/search）\",\n");
        prompt.append("  \"params\": {\"工具参数\": \"参数值\"},\n");
        prompt.append("  \"result\": \"初始结果（可为空）\"\n");
        prompt.append("}\n\n");

        prompt.append("【示例1 - 天气查询】\n");
        prompt.append("{\"thought\": \"用户询问天气，需要调用weather工具\",");
        prompt.append("\"action\": \"weather\",");
        prompt.append("\"params\": {\"city\": \"汉中\"},");
        prompt.append("\"result\": \"\"");
        prompt.append("}\n\n");

        prompt.append("【示例2 - 普通对话】\n");
        prompt.append("{\"thought\": \"用户是问候，直接回答即可\",");
        prompt.append("\"action\": \"chat\",");
        prompt.append("\"params\": {\"message\": \"").append(userMessage).append("\"},");
        prompt.append("\"result\": \"\"");
        prompt.append("}\n\n");

        prompt.append("【示例3 - 联网搜索】\n");
        prompt.append("{\"thought\": \"用户想搜索新闻，需要联网搜索\",");
        prompt.append("\"action\": \"search\",");
        prompt.append("\"params\": {\"query\": \"学校新闻\"},");
        prompt.append("\"result\": \"\"");
        prompt.append("}\n");

        return prompt.toString();
    }

    private String buildContinuePrompt(String originalPrompt, String toolResult, Map<String, Object> toolResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(originalPrompt).append("\n\n");

        prompt.append("【工具执行结果】\n");
        for (Map.Entry<String, Object> entry : toolResults.entrySet()) {
            prompt.append("【").append(entry.getKey()).append("】\n");
            prompt.append(entry.getValue()).append("\n\n");
        }

        prompt.append("【继续指令】\n");
        prompt.append("请根据工具执行结果，决定下一步操作：\n");
        prompt.append("- 如果已有答案 → 输出 {\"action\": \"answer\", \"result\": \"最终答案\"}\n");
        prompt.append("- 如果需要更多信息 → 继续调用工具\n");
        prompt.append("- 如果是直接对话 → 输出 {\"action\": \"chat\", \"params\": {}}");

        return prompt.toString();
    }

    private String buildHistory(UserContext context, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return "";
        try {
            List<RedisSessionManager.ChatMessage> history = redisSessionManager.getHistory(context.getUserId(), sessionId);
            if (history.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (RedisSessionManager.ChatMessage msg : history) {
                if (count >= MAX_HISTORY) break;
                String role = msg.role().equals("user") ? "用户" : "AI";
                sb.append(role).append(": ").append(msg.content()).append("\n");
                count++;
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取历史失败", e);
            return "";
        }
    }

    private String callReActLLM(String prompt, UserContext context) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("enable_search", false);
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
            log.error("ReAct LLM调用失败", e);
        }
        return null;
    }

    private ReActStep parseReActResponse(String response) {
        try {
            int startIdx = response.indexOf('{');
            int endIdx = response.lastIndexOf('}');
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonStr = response.substring(startIdx, endIdx + 1);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);

                String thought = (String) parsed.getOrDefault("thought", "");
                String action = (String) parsed.getOrDefault("action", "answer");
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) parsed.getOrDefault("params", new HashMap<>());
                String result = (String) parsed.getOrDefault("result", "");

                return new ReActStep(thought, action, params, result);
            }
        } catch (Exception e) {
            log.warn("解析ReAct响应失败", e);
        }
        return null;
    }

    private String executeWeather(Map<String, Object> params, UserContext context) {
        try {
            ResultMessage result = weatherAgent.execute("query", context, params);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "天气查询失败";
        } catch (Exception e) {
            return "天气服务暂时不可用";
        }
    }

    private String executeExpense(Map<String, Object> params, UserContext context) {
        try {
            ResultMessage result = expenseAgent.execute("create", context, params);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "记账失败";
        } catch (Exception e) {
            return "记账服务暂时不可用";
        }
    }

    private String executeCourse(Map<String, Object> params, UserContext context, Map<String, String> files) {
        try {
            ResultMessage result = courseAgent.execute("query", context, params, files);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "课程查询失败";
        } catch (Exception e) {
            return "课程服务暂时不可用";
        }
    }

    private String executeProfile(Map<String, Object> params, UserContext context) {
        try {
            ResultMessage result = profileAgent.execute("update", context, params);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "资料修改失败";
        } catch (Exception e) {
            return "资料服务暂时不可用";
        }
    }

    private String executeNote(Map<String, Object> params, UserContext context) {
        try {
            ResultMessage result = noteAgent.execute("generate", context, params);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "笔记生成失败";
        } catch (Exception e) {
            return "笔记服务暂时不可用";
        }
    }

    private String executeCalorie(Map<String, Object> params, UserContext context) {
        try {
            ResultMessage result = calorieAgent.execute("create", context, params);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "卡路里记录失败";
        } catch (Exception e) {
            return "卡路里服务暂时不可用";
        }
    }

    private String executeChat(Map<String, Object> params, UserContext context, String sessionId) {
        try {
            Map<String, Object> chatParams = new HashMap<>(params);
            chatParams.put("sessionId", sessionId);
            ResultMessage result = chatAgent.execute("chat", context, chatParams);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "对话处理失败";
        } catch (Exception e) {
            return "对话服务暂时不可用";
        }
    }

    private String executeSearch(Map<String, Object> params, UserContext context) {
        try {
            ResultMessage result = searchAgent.execute("search", context, params);
            return result.isSuccess() ? String.valueOf(result.getResult()) : "搜索失败";
        } catch (Exception e) {
            return "搜索服务暂时不可用";
        }
    }

    private record ReActStep(String thought, String actionType, Map<String, Object> params, String result) {}
}
