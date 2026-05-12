package com.qingtu.agent.agent;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent记忆配置 - LangChain4j ChatMemory
 * 滑动窗口记忆，保留最近对话 + 用户画像
 */
@Configuration
public class AgentMemoryConfig {

    @Value("${langchain4j.history.max-messages:10}")
    private int maxMessages;

    /**
     * 创建Agent级别ChatMemory Bean
     * 自动保留最近N轮对话
     */
    @Bean
    ChatMemory agentChatMemory() {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }
}