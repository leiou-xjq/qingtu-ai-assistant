package com.qingtu.agent.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一AI客户端
 * - 豆包模型
 * - 本地兜底回复
 */
@Slf4j
@Component
public class AiClient {

    // 豆包配置
    @Value("${ai.doubao.api-key:}")
    private String doubaoApiKey;

    @Value("${ai.doubao.model:ep-20260428080756-pbltx}")
    private String doubaoModel;

    @Value("${ai.doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}")
    private String doubaoBaseUrl;

    /**
     * 通用AI对话（兼容原有调用）
     */
    public String chat(String prompt) {
        return chatWithMemory(prompt, null);
    }

    /**
     * 带上下文的AI对话（豆包优先）
     */
    public String chatWithMemory(String prompt, List<String> context) {
        try {
            // 优先尝试豆包
            if (doubaoApiKey != null && !doubaoApiKey.isEmpty()) {
                try {
                    return callDoubao(prompt);
                } catch (Exception e) {
                    log.warn("豆包调用失败: {}", e.getMessage());
                }
            }

            return generateFallbackResponse(prompt);

        } catch (Exception e) {
            log.error("AI调用异常: {}", e.getMessage());
            return generateFallbackResponse(prompt);
        }
    }

    /**
     * 生成JSON格式响应
     */
    public String generateJson(String instruction, String schema) {
        String prompt = instruction + "\n\n请以JSON格式返回，格式：" + schema;
        return chat(prompt);
    }

    /**
     * 调用豆包模型
     */
    private String callDoubao(String prompt) {
        String url = doubaoBaseUrl + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", doubaoModel);

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", "你是青途智伴AI助手，管理你的学习和生活，陪你聊天谈心，有任何问题都可以问我。"));
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + doubaoApiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<Map<String, Object>> entity =
            new org.springframework.http.HttpEntity<>(requestBody, headers);

        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        String response = rt.postForObject(url, entity, String.class);

        return parseResponse(response);
    }

    /**
     * 解析AI响应
     */
    private String parseResponse(String response) {
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);

            // 尝试多种响应格式
            com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isEmpty()) return content;
            }

            // DashScope格式
            choices = root.path("output").path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }

            return root.path("output").path("text").asText();
        } catch (Exception e) {
            log.error("解析响应失败: {}", response, e);
            throw new RuntimeException("AI响应解析失败");
        }
    }

    /**
     * 本地兜底响应
     */
    private String generateFallbackResponse(String prompt) {
        if (prompt.contains("食谱") || prompt.contains("推荐")) {
            return "抱歉，AI服务暂时不可用，请在设置中配置API Key后重新尝试。";
        } else if (prompt.contains("分析") || prompt.contains("报告")) {
            return "抱歉，AI服务暂时不可用，分析功能稍后可用。";
        } else if (prompt.contains("课程") || prompt.contains("笔记")) {
            return "抱歉，AI服务暂时不可用，请稍后再试。";
        } else {
            return "我是青途智伴AI助手，管理你的学习和生活，目前AI服务正在维护中，其他功能可正常使用。";
        }
    }

    /**
     * 检查AI服务是否可用
     */
    public boolean isAvailable() {
        return doubaoApiKey != null && !doubaoApiKey.isEmpty();
    }

    /**
     * 获取AI服务状态信息
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("AI服务状态:\n");
        sb.append(doubaoApiKey != null && !doubaoApiKey.isEmpty() ? "✓ 豆包AI: 已配置\n" : "✗ 豆包AI: 未配置\n");
        return sb.toString();
    }
}