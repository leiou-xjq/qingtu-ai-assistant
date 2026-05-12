package com.qingtu.agent.mcp.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MCP Server 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerConfig {

    private boolean enabled = true;
    private String version = "1.0";
    private int maxConcurrentRequests = 100;
    private long defaultTimeoutMs = 30000;

    private Security security = new Security();
    private Audit audit = new Audit();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Security {
        private boolean requireAuth = true;
        private String apiKeyHeader = "X-MCP-API-Key";
        private boolean enablePermissionCheck = true;
    }

    @Data
    public static class Audit {
        private boolean enabled = true;
        private int retentionDays = 90;
        private boolean logToolCalls = true;
        private boolean logUserId = true;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int defaultLimitPerHour = 1000;
        private Map<String, Integer> perToolLimits;
    }
}
