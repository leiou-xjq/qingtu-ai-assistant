package com.qingtu.agent.config;

import com.qingtu.agent.util.JwtUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 * 
 * 功能说明：
 * - 配置JWT密钥和过期时间
 * - 创建JwtUtil工具Bean
 * 
 * @author 青途智伴技术团队
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT密钥
     * 
     * 说明：
     * - 生产环境必须替换为复杂密钥
     * - 建议使用至少256位的密钥
     */
    private String secret = "qingtu-secret-key-change-in-production-2024";

    /**
     * Token过期时间（毫秒）
     * 
     * 说明：
     * - 默认7天
     * - 可配置更短的有效期提高安全性
     */
    private long expiration = 604800000L;

    /**
     * Token Header名称
     */
    private String header = "Authorization";

    /**
     * Token前缀
     */
    private String prefix = "Bearer ";

    /**
     * 创建JwtUtil工具Bean
     */
    @Bean
    public JwtUtil jwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        jwtUtil.setSecret(secret);
        jwtUtil.setExpiration(expiration);
        jwtUtil.setHeader(header);
        jwtUtil.setPrefix(prefix);
        return jwtUtil;
    }
}