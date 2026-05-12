package com.qingtu.agent.agent.message;

import com.qingtu.agent.agent.message.config.AgentQueueConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 消息发布者
 * Orchestrator 使用此类发布任务到各 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishWeather(TaskMessage message) {
        log.info("发布天气任务: taskId={}, userId={}", message.getTaskId(), message.getUserId());
        rabbitTemplate.convertAndSend(
                AgentQueueConfig.EXCHANGE_AGENT,
                AgentQueueConfig.ROUTING_KEY_WEATHER,
                message
        );
    }

    public void publishExpense(TaskMessage message) {
        log.info("发布记账任务: taskId={}, userId={}", message.getTaskId(), message.getUserId());
        rabbitTemplate.convertAndSend(
                AgentQueueConfig.EXCHANGE_AGENT,
                AgentQueueConfig.ROUTING_KEY_EXPENSE,
                message
        );
    }

    public void publishCourse(TaskMessage message) {
        log.info("发布课程任务: taskId={}, userId={}", message.getTaskId(), message.getUserId());
        rabbitTemplate.convertAndSend(
                AgentQueueConfig.EXCHANGE_AGENT,
                AgentQueueConfig.ROUTING_KEY_COURSE,
                message
        );
    }

    public void publishProfile(TaskMessage message) {
        log.info("发布资料任务: taskId={}, userId={}", message.getTaskId(), message.getUserId());
        rabbitTemplate.convertAndSend(
                AgentQueueConfig.EXCHANGE_AGENT,
                AgentQueueConfig.ROUTING_KEY_PROFILE,
                message
        );
    }

    public void publishNote(TaskMessage message) {
        log.info("发布笔记任务: taskId={}, userId={}", message.getTaskId(), message.getUserId());
        rabbitTemplate.convertAndSend(
                AgentQueueConfig.EXCHANGE_AGENT,
                AgentQueueConfig.ROUTING_KEY_NOTE,
                message
        );
    }

    public void publish(TaskMessage message) {
        switch (message.getAgent().toLowerCase()) {
            case "weather" -> publishWeather(message);
            case "expense" -> publishExpense(message);
            case "course" -> publishCourse(message);
            case "profile" -> publishProfile(message);
            case "note" -> publishNote(message);
            default -> log.warn("未知Agent类型: {}", message.getAgent());
        }
    }
}