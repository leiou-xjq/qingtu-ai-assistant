package com.qingtu.agent.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存配置类
 * 
 * 功能说明：
 * - 配置进程内高性能缓存
 * - 缓存天气、课程等高频访问数据
 * - 避免频繁查询数据库或调用外部API
 * 
 * @author 青途智伴技术团队
 */
@Configuration
public class CacheConfig {

    /**
     * 配置Caffeine缓存管理器
     * 
     * 说明：
     * - 最大缓存数量：1000条
     * - 写入后过期时间：1小时
     * - 组合配置：最大数量1000且1小时后过期
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats();
        
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

}
