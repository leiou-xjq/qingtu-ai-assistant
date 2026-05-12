package com.qingtu.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Component
public class RedisCacheUtil {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheUtil.class);

    public static final String OUTFIT_MALE = "qtu:outfit:male";
    public static final String OUTFIT_FEMALE = "qtu:outfit:female";
    public static final String RECIPE_PREFIX = "qtu:recipe:";
    private static final String OUTFIT_KEY_PREFIX = "qtu:outfit:";

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final String TIMESTAMP_SUFFIX = ":_ts";

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisCacheUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setOutfit(String city, String gender, String content) {
        String key = OUTFIT_KEY_PREFIX + city + ":" + gender;
        set(key, content, DEFAULT_TTL);
    }

    public String getOutfit(String city, String gender) {
        String key = OUTFIT_KEY_PREFIX + city + ":" + gender;
        return get(key);
    }

    public void deleteOldOutfitCache() {
        try {
            Set<String> keys = redisTemplate.keys("qtu:outfit:male");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清理旧穿搭缓存: 删除{}个key", keys.size());
            }
        } catch (Exception e) {
            log.error("清理旧穿搭缓存失败", e);
        }
    }

    public void setRecipe(Long userId, String content) {
        String key = RECIPE_PREFIX + userId;
        set(key, content, DEFAULT_TTL);
    }

    public void setRecipeWithTime(Long userId, String content, LocalDateTime executeTime) {
        String key = RECIPE_PREFIX + userId;
        String tsKey = key + TIMESTAMP_SUFFIX;
        setWithTimestamp(key, content, executeTime, tsKey);
    }

    public String getRecipe(Long userId) {
        String key = RECIPE_PREFIX + userId;
        return get(key);
    }

    public void deleteRecipe(Long userId) {
        String key = RECIPE_PREFIX + userId;
        delete(key);
        delete(key + TIMESTAMP_SUFFIX);
    }

    private void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("缓存设置成功: key={}", key);
        } catch (Exception e) {
            log.error("缓存设置失败: key={}, error={}", key, e.getMessage());
        }
    }

    private void setWithTimestamp(String key, String value, LocalDateTime executeTime, String tsKey) {
        Duration remainingTTL = calculateRemainingTTL(executeTime);
        if (remainingTTL.toMinutes() <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, remainingTTL);
            redisTemplate.opsForValue().set(tsKey, executeTime.toString(), remainingTTL);
            log.debug("缓存设置成功: key={}, remainingTTL={}h", key, remainingTTL.toHours());
        } catch (Exception e) {
            log.error("缓存设置失败: key={}, error={}", key, e.getMessage());
        }
    }

    private Duration calculateRemainingTTL(LocalDateTime executeTime) {
        if (executeTime == null) {
            return DEFAULT_TTL;
        }
        LocalDateTime now = LocalDateTime.now();
        long hours = Duration.between(now, executeTime.plusHours(24)).toHours();
        if (hours <= 0) {
            return Duration.ofMinutes(5);
        }
        return Duration.ofHours(Math.min(hours, 24));
    }

    private String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("缓存获取失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    private void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("缓存删除成功: key={}", key);
        } catch (Exception e) {
            log.error("缓存删除失败: key={}, error={}", key, e.getMessage());
        }
    }

    public Boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("缓存检查失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }
}