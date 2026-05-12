package com.qingtu.agent.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSessionManager {

    private final StringRedisTemplate redisTemplate;

    private static final String SESSION_KEY_PREFIX = "ai:session:";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    public String createSession(Long userId) {
        String sessionId = java.util.UUID.randomUUID().toString();
        String key = buildKey(userId, sessionId);

        redisTemplate.opsForHash().put(key, "createdAt", java.time.LocalDateTime.now().toString());
        redisTemplate.opsForHash().put(key, "lastActiveAt", java.time.LocalDateTime.now().toString());
        redisTemplate.opsForValue().set(key + ":history", "[]", DEFAULT_TTL);

        redisTemplate.expire(key, Duration.ofHours(24));
        log.info("创建会话: userId={}, sessionId={}", userId, sessionId);
        return sessionId;
    }

    public void appendMessage(Long userId, String sessionId, MessageRole role, String content) {
        String key = buildKey(userId, sessionId);
        String historyKey = key + ":history";

        String historyJson = redisTemplate.opsForValue().get(historyKey);
        if (historyJson == null) {
            historyJson = "[]";
        }

        List<ChatMessage> messages = parseHistory(historyJson);
        messages.add(new ChatMessage(role.name().toLowerCase(), content));

        if (messages.size() > MAX_HISTORY_SIZE) {
            messages = summarizeHistory(messages);
        }

        String newJson = toJson(messages);
        redisTemplate.opsForValue().set(historyKey, newJson, DEFAULT_TTL);
        redisTemplate.opsForHash().put(key, "lastActiveAt", java.time.LocalDateTime.now().toString());
    }

    public List<ChatMessage> getHistory(Long userId, String sessionId) {
        String historyKey = buildKey(userId, sessionId) + ":history";
        String historyJson = redisTemplate.opsForValue().get(historyKey);

        if (historyJson == null || historyJson.isBlank()) {
            return new ArrayList<>();
        }

        return parseHistory(historyJson);
    }

    public void refreshTTL(Long userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        redisTemplate.expire(key, DEFAULT_TTL);
        redisTemplate.expire(key + ":history", DEFAULT_TTL);
    }

    public void deleteSession(Long userId, String sessionId) {
        String key = buildKey(userId, sessionId);
        redisTemplate.delete(key);
        redisTemplate.delete(key + ":history");
        log.info("删除会话: userId={}, sessionId={}", userId, sessionId);
    }

    private String buildKey(Long userId, String sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private List<ChatMessage> parseHistory(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class));
        } catch (Exception e) {
            log.error("解析历史记录失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toJson(List<ChatMessage> messages) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(messages);
        } catch (Exception e) {
            log.error("序列化历史记录失败: {}", e.getMessage());
            return "[]";
        }
    }

    private List<ChatMessage> summarizeHistory(List<ChatMessage> messages) {
        return messages.subList(messages.size() - MAX_HISTORY_SIZE, messages.size());
    }

    public record ChatMessage(String role, String content) {}
    public enum MessageRole { USER, ASSISTANT, SYSTEM }
}