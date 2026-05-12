package com.qingtu.agent.config;

import com.qingtu.agent.service.OutfitCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动时初始化穿搭缓存
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class OutfitCacheInitializer implements ApplicationRunner {

    private final OutfitCacheService outfitCacheService;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        log.info("【应用启动】开始初始化穿搭缓存...");

        try {
            outfitCacheService.initializeAllCities();
            log.info("【应用启动】穿搭缓存初始化完成");
        } catch (Exception e) {
            log.error("【应用启动】穿搭缓存初始化失败", e);
        }
    }
}
