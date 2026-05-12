package com.qingtu.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 和风天气API配置类
 * 
 * 功能说明：
 * - 配置天气API密钥
 * - 配置请求基础地址
 * - 配置超时时间
 * 
 * @author 青途智伴技术团队
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "weather")
public class WeatherConfig {

    /**
     * 和风天气API Key
     * 
     * 获取地址：https://dev.qweather.com/
     * 免费版每日额度：1000次
     */
    private String apiKey;

    /**
     * API基础地址
     */
    private String baseUrl = "https://devapi.qweather.com/v7";

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 10000;

    /**
     * 是否启用天气功能
     */
    private boolean enabled = true;
}