package com.qingtu.agent.util;

import com.qingtu.agent.common.Constants;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 
 * 功能说明：
 * - 基于Redis实现分布式锁
 * - 防止定时任务重复执行
 * - 支持锁自动过期
 * 
 * 使用方式：
 * - tryLock(key): 尝试获取锁
 * - unlock(key): 释放锁
 * 
 * @author 青途智伴技术团队
 */
public class RedisLockUtil {

    private static StringRedisTemplate stringRedisTemplate;

    public static void setStringRedisTemplate(StringRedisTemplate template) {
        stringRedisTemplate = template;
    }

    /**
     * 尝试获取分布式锁
     * 
     * @param lockKey 锁的键
     * @param expireSeconds 过期时间（秒）
     * @return 是否获取成功
     */
    public static boolean tryLock(String lockKey, long expireSeconds) {
        if (stringRedisTemplate == null) {
            return true;
        }
        String lockValue = String.valueOf(System.currentTimeMillis());
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放分布式锁
     */
    public static void unlock(String lockKey) {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 尝试获取任务锁（带前缀）
     * 
     * @param taskKey 任务标识
     * @return 是否获取成功
     */
    public static boolean tryTaskLock(String taskKey) {
        return tryLock(Constants.TASK_LOCK_PREFIX + taskKey, Constants.TASK_LOCK_EXPIRE);
    }

    /**
     * 释放任务锁
     */
    public static void unlockTask(String taskKey) {
        unlock(Constants.TASK_LOCK_PREFIX + taskKey);
    }

    /**
     * 执行带锁的任务
     * 
     * @param taskKey 任务标识
     * @param runnable 任务执行逻辑
     * @return 是否执行（false表示被其他实例执行）
     */
    public static boolean executeWithLock(String taskKey, Runnable runnable) {
        if (tryTaskLock(taskKey)) {
            try {
                runnable.run();
                return true;
            } finally {
                unlockTask(taskKey);
            }
        }
        return false;
    }
}