package com.qingtu.agent.application.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.qingtu.agent.infrastructure.messaging.RabbitMQConfig;

@Slf4j
@Component
public class ConversationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ConversationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
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

    @Async
    public void publishConversation(Long userId, String sessionId, String userMessage,
                                 String assistantMessage, String intent,
                                 String toolUsed, long latencyMs) {
        try {
            ConversationEvent userEvent = new ConversationEvent(
                null, userId, sessionId, "user", userMessage, intent, (int) latencyMs
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_AI,
                RabbitMQConfig.ROUTING_CONVERSATION,
                userEvent
            );

            ConversationEvent assistantEvent = new ConversationEvent(
                null, userId, sessionId, "assistant", assistantMessage, intent, (int) latencyMs
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_AI,
                RabbitMQConfig.ROUTING_CONVERSATION,
                assistantEvent
            );
            log.debug("发布对话事件: userId={}, intent={}", userId, intent);
        } catch (Exception e) {
            log.error("发布对话事件失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Async
    public void publishToolInvocation(Long userId, String toolName, boolean success, long durationMs) {
        try {
            ConversationEvent event = new ConversationEvent(
                null, userId, null, "system",
                "Tool:" + toolName + " Result:" + (success ? "success" : "failed"),
                "TOOL", (int) durationMs
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_AI,
                RabbitMQConfig.ROUTING_TOOL_INVOKE,
                event
            );
        } catch (Exception e) {
            log.error("发布工具事件失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Async
    public void publishRagSearch(Long userId, String query, int resultCount, long durationMs) {
        try {
            ConversationEvent event = new ConversationEvent(
                null, userId, null, "system",
                "RAG Query:" + query + " Results:" + resultCount,
                "RAG", (int) durationMs
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_AI,
                RabbitMQConfig.ROUTING_RAG_SEARCH,
                event
            );
        } catch (Exception e) {
            log.error("发布RAG事件失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Async
    public void publishAlert(Long userId, String level, String message, String traceId) {
        try {
            ConversationEvent event = new ConversationEvent(
                traceId, userId, null, "system",
                level + ":" + message, "ALERT", 0
            );
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_AI,
                RabbitMQConfig.ROUTING_ALERT,
                event
            );
            log.warn("发布告警事件: userId={}, level={}, message={}", userId, level, message);
        } catch (Exception e) {
            log.error("发布告警事件失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}