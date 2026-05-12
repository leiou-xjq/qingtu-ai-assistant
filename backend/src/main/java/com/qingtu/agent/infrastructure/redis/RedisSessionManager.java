package com.qingtu.agent.infrastructure.redis;

import com.qingtu.agent.entity.po.ChatMessage;
import com.qingtu.agent.mapper.ChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private final ChatMessageMapper chatMessageMapper;

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

        List<ChatRecord> messages = parseHistory(historyJson);
        messages.add(new ChatRecord(role.name().toLowerCase(), content));

        if (messages.size() > MAX_HISTORY_SIZE) {
            messages = summarizeHistory(messages);
        }

        String newJson = toJson(messages);
        redisTemplate.opsForValue().set(historyKey, newJson, DEFAULT_TTL);
        redisTemplate.opsForHash().put(key, "lastActiveAt", java.time.LocalDateTime.now().toString());
    }

    public List<ChatRecord> getHistory(Long userId, String sessionId) {
        String historyKey = buildKey(userId, sessionId) + ":history";
        String historyJson = redisTemplate.opsForValue().get(historyKey);

        if (historyJson != null && !historyJson.isBlank()) {
            return parseHistory(historyJson);
        }

        List<ChatRecord> mysqlHistory = loadFromMySQL(sessionId);
        if (!mysqlHistory.isEmpty()) {
            restoreToRedis(userId, sessionId, mysqlHistory);
        }

        return mysqlHistory;
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

    private List<ChatRecord> loadFromMySQL(String sessionId) {
        try {
            Long sid = Long.parseLong(sessionId);
            List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, sid)
                    .orderByAsc(ChatMessage::getCreatedAt)
            );

            List<ChatRecord> records = new ArrayList<>();
            for (ChatMessage msg : messages) {
                records.add(new ChatRecord(
                    "user".equals(msg.getRole()) ? "user" : "assistant",
                    msg.getContent()
                ));
            }

            if (records.size() > MAX_HISTORY_SIZE) {
                records = records.subList(records.size() - MAX_HISTORY_SIZE, records.size());
            }

            return records;
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
    }

    private void restoreToRedis(Long userId, String sessionId, List<ChatRecord> records) {
        try {
            String historyKey = buildKey(userId, sessionId) + ":history";
            String json = toJson(records);
            redisTemplate.opsForValue().set(historyKey, json, DEFAULT_TTL);
            log.debug("MySQL历史已回填Redis: userId={}, sessionId={}, count={}", userId, sessionId, records.size());
        } catch (Exception e) {
            log.warn("回填Redis失败: {}", e.getMessage());
        }
    }

    private String buildKey(Long userId, String sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private List<ChatRecord> parseHistory(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, ChatRecord.class));
        } catch (Exception e) {
            log.error("解析历史记录失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toJson(List<ChatRecord> messages) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(messages);
        } catch (Exception e) {
            log.error("序列化历史记录失败: {}", e.getMessage());
            return "[]";
        }
    }

    private List<ChatRecord> summarizeHistory(List<ChatRecord> messages) {
        return messages.subList(messages.size() - MAX_HISTORY_SIZE, messages.size());
    }

    public record ChatRecord(String role, String content) {}
    public enum MessageRole { USER, ASSISTANT, SYSTEM }
}
