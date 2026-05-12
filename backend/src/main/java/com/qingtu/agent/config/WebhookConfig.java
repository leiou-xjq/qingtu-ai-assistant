package com.qingtu.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Webhook推送配置类
 * 
 * 功能说明：
 * - 配置钉钉Webhook
 * - 配置企业微信Webhook
 * - 支持消息推送功能
 * 
 * @author 青途智伴技术团队
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookConfig {

    /**
     * 钉钉Webhook配置
     */
    private Dingtalk dingtalk = new Dingtalk();

    /**
     * 企业微信Webhook配置
     */
    private WorkWeixin workweixin = new WorkWeixin();

    @Data
    public static class Dingtalk {
        /**
         * 是否启用钉钉推送
         */
        private boolean enabled = false;

        /**
         * 钉钉Webhook地址
         */
        private String url;

        /**
         * 钉钉加签密钥（可选）
         */
        private String secret;
    }

    @Data
    public static class WorkWeixin {
        /**
         * 是否启用企业微信推送
         */
        private boolean enabled = false;

        /**
         * 企业微信Webhook地址
         */
        private String webhookUrl;
    }
}