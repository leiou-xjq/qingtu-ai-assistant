package com.qingtu.agent.service;

import com.qingtu.agent.util.WeatherUtil;

/**
 * 穿搭缓存服务
 */
public interface OutfitCacheService {

    void refreshOutfitForCity(String city);

    void initializeAllCities();
}
