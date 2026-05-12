package com.qingtu.agent.agent.message.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 消息队列配置
 * 定义各 Agent 的队列、交换机和绑定关系
 */
@Configuration
public class AgentQueueConfig {

    public static final String QUEUE_WEATHER = "agent.weather";
    public static final String QUEUE_EXPENSE = "agent.expense";
    public static final String QUEUE_COURSE = "agent.course";
    public static final String QUEUE_PROFILE = "agent.profile";
    public static final String QUEUE_NOTE = "agent.note";
    public static final String QUEUE_RESULTS = "agent.results";

    public static final String EXCHANGE_AGENT = "agent.exchange";
    public static final String EXCHANGE_RESULTS = "agent.results.exchange";

    public static final String ROUTING_KEY_WEATHER = "agent.weather";
    public static final String ROUTING_KEY_EXPENSE = "agent.expense";
    public static final String ROUTING_KEY_COURSE = "agent.course";
    public static final String ROUTING_KEY_PROFILE = "agent.profile";
    public static final String ROUTING_KEY_NOTE = "agent.note";
    public static final String ROUTING_KEY_RESULTS = "agent.results";

    @Bean
    public DirectExchange agentExchange() {
        return new DirectExchange(EXCHANGE_AGENT);
    }

    @Bean
    public DirectExchange resultsExchange() {
        return new DirectExchange(EXCHANGE_RESULTS);
    }

    @Bean
    public Queue weatherQueue() {
        return QueueBuilder.durable(QUEUE_WEATHER).build();
    }

    @Bean
    public Queue expenseQueue() {
        return QueueBuilder.durable(QUEUE_EXPENSE).build();
    }

    @Bean
    public Queue courseQueue() {
        return QueueBuilder.durable(QUEUE_COURSE).build();
    }

    @Bean
    public Queue profileQueue() {
        return QueueBuilder.durable(QUEUE_PROFILE).build();
    }

    @Bean
    public Queue noteQueue() {
        return QueueBuilder.durable(QUEUE_NOTE).build();
    }

    @Bean
    public Queue resultsQueue() {
        return QueueBuilder.durable(QUEUE_RESULTS).build();
    }

    @Bean
    public Binding weatherBinding() {
        return BindingBuilder.bind(weatherQueue()).to(agentExchange()).with(ROUTING_KEY_WEATHER);
    }

    @Bean
    public Binding expenseBinding() {
        return BindingBuilder.bind(expenseQueue()).to(agentExchange()).with(ROUTING_KEY_EXPENSE);
    }

    @Bean
    public Binding courseBinding() {
        return BindingBuilder.bind(courseQueue()).to(agentExchange()).with(ROUTING_KEY_COURSE);
    }

    @Bean
    public Binding profileBinding() {
        return BindingBuilder.bind(profileQueue()).to(agentExchange()).with(ROUTING_KEY_PROFILE);
    }

    @Bean
    public Binding noteBinding() {
        return BindingBuilder.bind(noteQueue()).to(agentExchange()).with(ROUTING_KEY_NOTE);
    }

    @Bean
    public Binding resultsBinding() {
        return BindingBuilder.bind(resultsQueue()).to(resultsExchange()).with(ROUTING_KEY_RESULTS);
    }
}