package com.qingtu.agent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "school")
@Data
public class SchoolConfig {
    private String apiUrl;
    private String ak;
    private int timeout = 10000;
}