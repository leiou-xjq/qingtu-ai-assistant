package com.qingtu.agent.application.listener;

import com.qingtu.agent.entity.po.ConversationLog;
import com.qingtu.agent.infrastructure.messaging.RabbitMQConfig;
import com.qingtu.agent.mapper.ConversationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationArchiveListener {

    private final ConversationLogMapper conversationLogMapper;

    @RabbitListener(queues = "conversation.archive.queue")
    public void handleConversationArchive(ConversationEvent event) {
        try {
            ConversationLog log = new ConversationLog();
            log.setTraceId(event.traceId);
            log.setUserId(event.userId());
            log.setSessionId(event.sessionId);
            log.setRole(event.role());
            log.setContent(event.content());
            log.setIntent(event.intent());
            log.setLatencyMs(event.latencyMs());
            log.setCreatedAt(LocalDateTime.now());

            conversationLogMapper.insert(log);
        } catch (Exception e) {
            log.error("对话存档失败: {}", e.getMessage(), e);
        }
    }

    public record ConversationEvent(
        String traceId,
        Long userId,
        String sessionId,
        String role,
        String content,
        String intent,
        Integer latencyMs
    ) {}
}