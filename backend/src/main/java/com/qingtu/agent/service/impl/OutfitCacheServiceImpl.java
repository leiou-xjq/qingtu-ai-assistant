package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.service.OutfitCacheService;
import com.qingtu.agent.util.RedisCacheUtil;
import com.qingtu.agent.util.RedisLockUtil;
import com.qingtu.agent.util.WeatherUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitCacheServiceImpl implements OutfitCacheService {

    private final UserMapper userMapper;
    private final WeatherUtil weatherUtil;
    private final QingTuAgent agent;
    private final RedisCacheUtil redisCacheUtil;

    private static final String DEFAULT_OUTFIT = "上衣: T恤或薄衬衫\n下装: 牛仔裤或休闲长裤\n鞋子: 运动鞋\n配件: 双肩包\n带伞: 可携带\n提示: 今日气温适中，建议分层穿搭，早晚可添加薄外套";

    @Override
    public void refreshOutfitForCity(String city) {
        String lockKey = "lock:outfit:refresh:" + city;
        if (!RedisLockUtil.tryLock(lockKey, 60)) {
            log.info("城市{}正在刷新中，跳过", city);
            return;
        }

        try {
            doRefreshOutfitForCity(city);
        } finally {
            RedisLockUtil.unlock(lockKey);
        }
    }

    private void doRefreshOutfitForCity(String city) {
        log.info("刷新城市穿搭缓存: {}", city);

        String weatherContext = "";

        try {
            WeatherUtil.WeatherInfo weather = weatherUtil.getCurrentWeather(city);
            weatherContext = String.format("当前城市：%s，气温：%s°C，天气：%s，体感：%s°C，湿度：%s%%",
                    city, weather.getTemp(), weather.getText(), weather.getFeelsLike(), weather.getHumidity());
            log.info("获取城市{}天气成功: {}°C, {}", city, weather.getTemp(), weather.getText());
        } catch (Exception e) {
            log.warn("获取城市{}天气信息失败: {}", city, e.getMessage());
            weatherContext = "天气数据获取失败，使用默认建议";
}

        try {
            String maleInstruction = weatherContext + "。请为男性生成穿搭建议。请严格按照以下格式返回（每行一个项目，用英文冒号分隔）：\n上衣: \n下装: \n鞋子: \n配件: \n带伞: \n提示: ";
            String maleResult = agent.chat(maleInstruction, null);
            redisCacheUtil.setOutfit(city, "M", maleResult);
            log.info("城市{}男性穿搭建议已刷新缓存", city);
        } catch (Exception e) {
            log.error("城市{}男性穿搭生成失败: {}", city, e.getMessage());
            redisCacheUtil.setOutfit(city, "M", DEFAULT_OUTFIT);
        }

        try {
            String femaleInstruction = weatherContext + "。请为女性生成穿搭建议。请严格按照以下格式返回（每行一个项目，用英文冒号分隔）：\n上衣: \n下装: \n鞋子: \n配件: \n带伞: \n提示: ";
            String femaleResult = agent.chat(femaleInstruction, null);
            redisCacheUtil.setOutfit(city, "F", femaleResult);
            log.info("城市{}女性穿搭建议已刷新缓存", city);
        } catch (Exception e) {
            log.error("城市{}女性穿搭生成失败: {}", city, e.getMessage());
            redisCacheUtil.setOutfit(city, "F", DEFAULT_OUTFIT);
        }
    }

    @Override
    public void initializeAllCities() {
        redisCacheUtil.deleteOldOutfitCache();
        log.info("旧穿搭缓存清理完成");

        Set<String> cities = getUniqueCities();
        if (cities.isEmpty()) {
            cities.add("北京");
            log.info("未找到用户城市，使用默认城市: 北京");
        }

        log.info("需要初始化的城市数量: {}", cities.size());

        for (String city : cities) {
            try {
                refreshOutfitForCity(city);
            } catch (Exception e) {
                log.error("城市{}穿搭初始化失败: {}", city, e.getMessage());
            }
        }

        log.info("穿搭缓存初始化完成，共{}个城市", cities.size());
    }

    private Set<String> getUniqueCities() {
        Set<String> cities = new HashSet<>();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(User::getCity);
        wrapper.ne(User::getCity, "");
        wrapper.eq(User::getDeleted, 0);
        wrapper.select(User::getCity);

        List<User> users = userMapper.selectList(wrapper);
        for (User user : users) {
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                cities.add(user.getCity().trim());
            }
        }

        return cities;
    }
}
