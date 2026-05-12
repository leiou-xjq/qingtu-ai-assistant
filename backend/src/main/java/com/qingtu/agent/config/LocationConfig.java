package com.qingtu.agent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "location")
@Data
public class LocationConfig {
    private String qqKey;
    private String apiUrl;
    private int timeout = 10000;
}