package com.qingtu.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.mcp.ContextHolder;
import com.qingtu.agent.tool.ToolCall;
import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolRegistry;
import com.qingtu.agent.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 青途AI助手核心Agent
 * 模型: 阿里云百炼 Qwen (qwen-plus), 支持内置联网搜索
 */
@Slf4j
@Component
public class QingTuAgent {

    private final DashScopeConfig dashScopeConfig;
    private final RestTemplate dashscopeRestTemplate;
    private final ObjectMapper objectMapper;
    private final AgentMemory memory;
    private final ToolRegistry toolRegistry;

    public QingTuAgent(DashScopeConfig dashScopeConfig, RestTemplate dashscopeRestTemplate, ToolRegistry toolRegistry) {
        this.dashScopeConfig = dashScopeConfig;
        this.dashscopeRestTemplate = dashscopeRestTemplate;
        this.objectMapper = new ObjectMapper();
        this.memory = new AgentMemory();
        this.toolRegistry = toolRegistry;
    }

    /** 同步对话 */
    public String chat(String userMessage, ContextHolder context) {
        return doChat(userMessage);
    }

    /** 同步对话（无上下文） */
    public String chat(String userMessage) {
        return doChat(userMessage);
    }

    private String doChat(String userMessage) {
        if (dashScopeConfig.isConfigured()) {
            try {
                String response = callQwenApi(userMessage);
                memory.save(userMessage, response);
                log.info("AI对话(Qwen) - 用户: {}", userMessage.substring(0, Math.min(30, userMessage.length())));
                return response;
            } catch (Exception e) {
                log.warn("Qwen调用失败: {}", e.getMessage());
            }
        }
        log.warn("Qwen未配置，使用模拟响应");
        return getMockResponse(userMessage);
    }

    /** 流式对话 */
    public Flux<String> chatStream(String userMessage) {
        if (dashScopeConfig.isConfigured()) {
            try {
                return callQwenStreamApi(userMessage);
            } catch (Exception e) {
                log.warn("Qwen流式调用失败: {}", e.getMessage());
            }
        }
        return Flux.just(getMockResponse(userMessage));
    }

    /** Qwen OpenAI 兼容流式调用 (enable_search) */
    private Flux<String> callQwenStreamApi(String userMessage) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));
        body.put("stream", true);
        body.put("enable_search", true);

        WebClient webClient = WebClient.create();
        return webClient.post()
            .uri(url)
            .header("Authorization", "Bearer " + dashScopeConfig.getApiKey())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .map(line -> {
                if ("[DONE]".equals(line.trim())) return "";
                try {
                    JsonNode node = objectMapper.readTree(line);
                    JsonNode choices = node.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        String content = choices.get(0).path("delta").path("content").asText("");
                        content = content.replace("\n", "").replace("\r", "");
                        content = content.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
                        content = content.replaceAll("\\*\\*", "");
                        content = content.replace("[DONE]", "").replace("\"", "");
                        content = content.replaceAll("\uD83C[\uDF00-\uDFFF]|\uD83D[\uDC00-\uDE4F]|\uD83E[\uDD00-\uDDFF]", "");
                        return content;
                    }
                } catch (Exception e) { return ""; }
                return "";
            })
            .filter(text -> !text.isEmpty())
            .doOnNext(token -> log.info("Qwen输出: {}", token));
    }

    /** Qwen 同步调用 */
    private String callQwenApi(String userMessage) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));
        body.put("enable_search", true);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String response = dashscopeRestTemplate.postForObject(url, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            log.error("解析Qwen响应失败", e);
        }
        return null;
    }

    public String chatWithRAG(String userMessage, String ragContext, ContextHolder context) {
        return chat(ragContext + "\n\n用户问题：" + userMessage, context);
    }

    public String generateJson(String instruction, String schema) {
        return chat(instruction + "\n\n请以JSON格式返回，格式：" + schema, null);
    }

    public AgentMemory getMemory() { return memory; }

    public void clearMemory() { memory.clear(); }

    /**
     * 工具调用对话
     * 支持 Function Calling / Tool Use 模式
     */
    public record ChatWithToolsResult(
            String response,
            List<ToolCall> toolCalls,
            boolean toolsUsed
    ) {}

    /**
     * 调用 LLM 支持工具（同步模式）
     * @param userMessage 用户消息
     * @param availableTools 可用工具名称列表（从 ToolRegistry 获取）
     * @return 对话结果
     */
    public ChatWithToolsResult chatWithTools(String userMessage, List<String> availableTools) {
        // 1. 获取工具定义
        List<Map<String, Object>> tools = availableTools.stream()
                .map(name -> toolRegistry.getToolDefinition(name).orElse(null))
                .filter(Objects::nonNull)
                .map(def -> Map.<String, Object>of(
                        "type", "function",
                        "function", Map.of(
                                "name", def.getName(),
                                "description", def.getDescription(),
                                "parameters", def.getParameters() != null ? def.getParameters() : new HashMap<>()
                        )
                ))
                .collect(Collectors.toList());

        // 2. 调用 LLM（携带 tools 参数）
        Map<String, Object> llmResponse = callQwenWithTools(userMessage, tools);

        // 3. 解析响应中的 tool_calls
        List<ToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = llmResponse.containsKey("tool_calls")
                ? (JsonNode) llmResponse.get("tool_calls")
                : null;

        if (toolCallsNode != null && toolCallsNode.isArray()) {
            for (JsonNode tc : toolCallsNode) {
                String name = tc.path("function").path("name").asText();
                JsonNode argsNode = tc.path("function").path("arguments");
                Map<String, Object> args = objectMapper.convertValue(argsNode, Map.class);
                toolCalls.add(new ToolCall(name, args));
            }
        }

        // 4. 如果有 tool_calls，执行工具并聚合结果
        String finalResponse;
        if (!toolCalls.isEmpty()) {
            Map<String, ToolResult> toolResults = toolRegistry.executeTools(toolCalls);
            String aggregatedPrompt = buildToolAggregationPrompt(userMessage, toolResults);
            finalResponse = chat(aggregatedPrompt);
        } else {
            finalResponse = llmResponse.containsKey("content")
                    ? (String) llmResponse.get("content")
                    : "";
        }

        return new ChatWithToolsResult(finalResponse, toolCalls, !toolCalls.isEmpty());
    }

    /**
     * 流式工具调用对话
     */
    public Flux<String> chatStreamWithTools(String userMessage, List<String> availableTools) {
        ChatWithToolsResult result = chatWithTools(userMessage, availableTools);
        if (result.toolsUsed()) {
            // 工具已执行，直接返回聚合后的响应
            return Flux.just(result.response());
        }
        // 无工具，走普通流式
        return chatStream(userMessage);
    }

    private Map<String, Object> callQwenWithTools(String userMessage, List<Map<String, Object>> tools) {
        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));
        body.put("enable_search", true);
        if (!tools.isEmpty()) {
            body.put("tools", tools);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String response = dashscopeRestTemplate.postForObject(url, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.path("message");

                // 检查是否有 tool_calls
                JsonNode toolCallsNode = message.path("tool_calls");
                if (toolCallsNode.isArray() && toolCallsNode.size() > 0) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("tool_calls", toolCallsNode);
                    return result;
                }

                // 普通文本响应
                Map<String, Object> result = new HashMap<>();
                result.put("content", message.path("content").asText());
                return result;
            }
        } catch (Exception e) {
            log.error("解析 Qwen 响应失败", e);
        }
        return Map.of("content", "");
    }

    private String buildToolAggregationPrompt(String originalMessage, Map<String, ToolResult> toolResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户原始问题：").append(originalMessage).append("\n\n");
        prompt.append("以下是工具执行结果：\n");

        toolResults.values().forEach(result -> {
            prompt.append("【").append(result.getToolName()).append("】\n");
            if (result.isSuccess()) {
                prompt.append("结果：").append(result.getData()).append("\n");
            } else {
                prompt.append("错误：").append(result.getErrorMessage()).append("\n");
            }
            prompt.append("\n");
        });

        prompt.append("\n请基于以上工具执行结果，回答用户问题。");
        return prompt.toString();
    }

    private String getMockResponse(String question) {
        if (question.contains("天气")) return "今日天气晴转多云，24C。";
        if (question.contains("课程") || question.contains("上课")) return "今日无课程安排。";
        if (question.contains("饮食") || question.contains("吃")) return "建议去食堂用餐。";
        if (question.contains("记账") || question.contains("花钱")) return "消费要量入为出。";
        return "我是青途AI助手，AI服务正在初始化中，请稍后重试。";
    }
}