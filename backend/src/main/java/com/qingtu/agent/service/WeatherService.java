package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

/**
 * 天气穿搭服务接口
 * 
 * 定义天气和穿搭相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface WeatherService {

    /**
     * 获取实时天气
     * 
     * @param userId 用户ID
     * @param location 城市名称（可选）
     * @return 天气信息
     */
    CommonResult<?> getCurrentWeather(Long userId, String location);

    /**
     * 获取天气预报
     * 
     * @param userId 用户ID
     * @param location 城市名称
     * @param days 天数
     * @return 天气预报
     */
    CommonResult<?> getForecast(Long userId, String location, int days);

    /**
     * 获取AI穿搭建议
     *
     * @param userId 用户ID
     * @param city 城市名（可选，如果为null则使用用户所在城市）
     * @return 穿搭建议
     */
    CommonResult<?> getOutfitSuggestion(Long userId, String city);

    /**
     * 异步预加载穿搭建议（后台执行，不阻塞）
     *
     * @param userId 用户ID
     */
    void preloadOutfitSuggestion(Long userId);

    /**
     * 发送早安推送
     *
     * @param userId 用户ID
     * @return 推送结果
     */
    CommonResult<?> sendMorningPush(Long userId);
}