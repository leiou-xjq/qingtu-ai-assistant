package com.qingtu.agent.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_AI = "ai.events";
    public static final String ROUTING_CONVERSATION = "conversation.archive";
    public static final String ROUTING_TOOL_INVOKE = "tool.invoke";
    public static final String ROUTING_RAG_SEARCH = "rag.search";
    public static final String ROUTING_ALERT = "alert.exception";

    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(EXCHANGE_AI);
    }

    @Bean
    public Queue conversationArchiveQueue() {
        return new Queue("conversation.archive.queue", true);
    }

    @Bean
    public Queue toolInvokeQueue() {
        return new Queue("tool.invoke.queue", true);
    }

    @Bean
    public Queue ragSearchQueue() {
        return new Queue("rag.search.queue", true);
    }

    @Bean
    public Queue alertQueue() {
        return new Queue("alert.queue", true);
    }

    @Bean
    public Binding conversationBinding(Queue conversationArchiveQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(conversationArchiveQueue).to(aiExchange).with(ROUTING_CONVERSATION);
    }

    @Bean
    public Binding toolBinding(Queue toolInvokeQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(toolInvokeQueue).to(aiExchange).with(ROUTING_TOOL_INVOKE);
    }

    @Bean
    public Binding ragBinding(Queue ragSearchQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(ragSearchQueue).to(aiExchange).with(ROUTING_RAG_SEARCH);
    }

    @Bean
    public Binding alertBinding(Queue alertQueue, TopicExchange aiExchange) {
        return BindingBuilder.bind(alertQueue).to(aiExchange).with(ROUTING_ALERT);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}