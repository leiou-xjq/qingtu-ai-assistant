package com.qingtu.agent.agent.agent;

import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.infrastructure.redis.RedisSessionManager;
import com.qingtu.agent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话Agent
 * 处理普通对话，支持上下文记忆
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSpecialistAgent {

    private final QingTuAgent qingTuAgent;
    private final RedisSessionManager redisSessionManager;
    private final UserMapper userMapper;

    private static final int MAX_HISTORY = 10;
    private static final int MAX_PROMPT_LENGTH = 4000;

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        String taskId = java.util.UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();
        String message = params.getOrDefault("message", "").toString();
        Long userId = context.getUserId();
        String sessionId = params.getOrDefault("sessionId", "").toString();

        try {
            String response = chatWithHistory(userId, sessionId, message, context);
            saveConversation(userId, sessionId, message, response);
            return ResultMessage.success(taskId, "chat", "chat", response, correlationId, userId);
        } catch (Exception e) {
            log.error("对话处理失败", e);
            return ResultMessage.failure(taskId, "chat", "chat", e.getMessage(), correlationId, userId);
        }
    }

    public String chatWithHistory(Long userId, String sessionId, String userMessage, UserContext context) {
        var user = userMapper.selectById(userId);
        String nickname = user != null && user.getNickname() != null ? user.getNickname() : "用户";
        String school = context.getCity();
        String city = context.getCity();

        List<RedisSessionManager.ChatMessage> history = getHistory(userId, sessionId);

        String prompt = buildPrompt(nickname, school, city, history, userMessage);
        return qingTuAgent.chat(prompt);
    }

    public List<RedisSessionManager.ChatMessage> getHistory(Long userId, String sessionId) {
        try {
            if (sessionId != null && !sessionId.isBlank()) {
                return redisSessionManager.getHistory(userId, sessionId);
            }
        } catch (Exception e) {
            log.warn("获取对话历史失败", e);
        }
        return List.of();
    }

    private String buildPrompt(String nickname, String school, String city, List<RedisSessionManager.ChatMessage> history, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是青途智伴，专为大学生打造的AI生活助手。你温暖、善解人意、充满正能量。\n\n");

        prompt.append("【用户信息】\n");
        prompt.append("- 昵称：").append(nickname).append("\n");
        if (school != null) prompt.append("- 学校：").append(school).append("\n");
        if (city != null) prompt.append("- 城市：").append(city).append("\n\n");

        if (!history.isEmpty()) {
            prompt.append("【对话历史】\n");
            int count = 0;
            for (RedisSessionManager.ChatMessage msg : history) {
                if (count >= MAX_HISTORY) break;
                prompt.append("用户: ").append(msg.content()).append("\n");
                prompt.append("AI: ").append(msg.role().equals("user") ? "[用户消息]" : msg.content()).append("\n\n");
                count++;
            }
        }

        prompt.append("【当前对话】\n");
        prompt.append("用户: ").append(userMessage).append("\n\n");
        prompt.append("请用温暖友好的语气回复，控制在150字以内。不要使用Markdown格式。");

        String result = prompt.toString();
        if (result.length() > MAX_PROMPT_LENGTH) {
            return result.substring(0, MAX_PROMPT_LENGTH);
        }
        return result;
    }

    private void saveConversation(Long userId, String sessionId, String userMessage, String aiResponse) {
        if (sessionId == null || sessionId.isBlank()) return;
        try {
            redisSessionManager.appendMessage(userId, sessionId,
                    RedisSessionManager.MessageRole.USER, userMessage);
            redisSessionManager.appendMessage(userId, sessionId,
                    RedisSessionManager.MessageRole.ASSISTANT, aiResponse);
        } catch (Exception e) {
            log.warn("保存对话历史失败", e);
        }
    }
}
