package com.qingtu.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ai.dashscope")
public class DashScopeConfig {

    private String apiKey;
    private String model = "qwen-plus";
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private int timeout = 60000;
    private int maxRetries = 3;

    @Bean
    public RestTemplate dashscopeRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        log.info("RestTemplate initialized, model: {}", model);
        return restTemplate;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}