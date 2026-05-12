package com.qingtu.agent.service.impl;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.WeatherService;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.util.WeatherUtil;
import com.qingtu.agent.util.RedisCacheUtil;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.mcp.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherUtil weatherUtil;
    private final UserMapper userMapper;
    private final UserHealthMapper userHealthMapper;
    private final QingTuAgent agent;
    private final RedisCacheUtil redisCacheUtil;

    private static final String DEFAULT_OUTFIT = "上衣: T恤或薄衬衫\n下装: 牛仔裤或休闲长裤\n鞋子: 运动鞋\n配件: 双肩包\n带伞: 可携带\n提示: 今日气温适中，建议分层穿搭，早晚可添加薄外套";

    @Override
    public CommonResult<?> getCurrentWeather(Long userId, String location) {
        String defaultCity = "北京";
        
        // 如果没有传入location，尝试从用户获取
        if (location == null || location.trim().isEmpty()) {
            if (userId != null && userId > 0) {
                User user = userMapper.selectById(userId);
                // 优先获取用户的city字段
                if (user != null && user.getCity() != null && !user.getCity().trim().isEmpty()) {
                    location = user.getCity();
                } else {
                    location = defaultCity;
                }
            } else {
                location = defaultCity;
            }
        }

        try {
            WeatherUtil.WeatherInfo weather = weatherUtil.getCurrentWeather(location);
            // 确保返回值不为null
            Map<String, Object> result = new HashMap<>();
            result.put("cityName", weather.getCityName() != null ? weather.getCityName() : location);
            result.put("temp", weather.getTemp() != null ? weather.getTemp() : "--");
            result.put("text", weather.getText() != null ? weather.getText() : "晴");
            result.put("feelsLike", weather.getFeelsLike() != null ? weather.getFeelsLike() : "--");
            result.put("humidity", weather.getHumidity() != null ? weather.getHumidity() : "--");
            result.put("wind", (weather.getWindDir() != null ? weather.getWindDir() : "") + (weather.getWindScale() != null ? weather.getWindScale() : ""));
            result.put("uvIndex", weather.getUvIndex() != null ? weather.getUvIndex() : "--");
            result.put("summary", weather.getWeatherSummary() != null ? weather.getWeatherSummary() : location + "天气");
            
            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("获取天气失败: {}", e.getMessage());
            Map<String, Object> fallbackResult = new HashMap<>();
            fallbackResult.put("cityName", location);
            fallbackResult.put("temp", "--");
            fallbackResult.put("text", "获取失败");
            fallbackResult.put("feelsLike", "--");
            fallbackResult.put("humidity", "--");
            fallbackResult.put("wind", "--");
            fallbackResult.put("uvIndex", "--");
            fallbackResult.put("summary", location + "天气数据获取中，请稍后刷新");
            return CommonResult.success(fallbackResult);
        }
    }

    @Override
    public CommonResult<?> getForecast(Long userId, String location, int days) {
        if (location == null || location.isEmpty()) {
            if (userId != null) {
                User user = userMapper.selectById(userId);
                location = user != null ? user.getCity() : "北京";
            } else {
                location = "北京";
            }
        }

        return CommonResult.success(weatherUtil.getForecast(location, days));
    }

    @Override
    public CommonResult<?> getOutfitSuggestion(Long userId, String city) {
        String location = city;
        if (location == null || location.isEmpty()) {
            if (userId != null && userId > 0) {
                User user = userMapper.selectById(userId);
                if (user != null && user.getCity() != null && !user.getCity().isEmpty()) {
                    location = user.getCity();
                }
            }
        }
        if (location == null || location.isEmpty()) {
            location = "北京";
        }

        String gender = "M";
        if (userId != null && userId > 0) {
            UserHealth health = userHealthMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserHealth>()
                    .eq(UserHealth::getUserId, userId)
                    .eq(UserHealth::getDeleted, 0));
            if (health != null && health.getGender() != null) {
                gender = health.getGender();
            }
        }

        String cached = redisCacheUtil.getOutfit(location, gender);
        if (cached != null && !cached.isEmpty()) {
            log.info("使用Redis缓存的穿搭建议, userId={}, city={}, gender={}", userId, location, gender);
            return CommonResult.success(cached);
        }

        String weatherContext = "";

        try {
            WeatherUtil.WeatherInfo weather = weatherUtil.getCurrentWeather(location);
            weatherContext = String.format("当前城市：%s，气温：%s°C，天气：%s，体感：%s°C，湿度：%s%%",
                    location, weather.getTemp(), weather.getText(), weather.getFeelsLike(), weather.getHumidity());
        } catch (Exception e) {
            log.warn("获取天气信息失败: {}", e.getMessage());
            weatherContext = "天气数据获取失败，使用默认建议";
        }

        try {
            String maleInstruction = weatherContext + "。请为男性生成穿搭建议。请严格按照以下格式返回（每行一个项目，用英文冒号分隔）：\n上衣: \n下装: \n鞋子: \n配件: \n带伞: \n提示: ";
            String maleResult = agent.chat(maleInstruction, null);
            redisCacheUtil.setOutfit(location, "M", maleResult);
            log.info("男性穿搭建议已缓存, city={}", location);

            String femaleInstruction = weatherContext + "。请为女性生成穿搭建议。请严格按照以下格式返回（每行一个项目，用英文冒号分隔）：\n上衣: \n下装: \n鞋子: \n配件: \n带伞: \n提示: ";
            String femaleResult = agent.chat(femaleInstruction, null);
            redisCacheUtil.setOutfit(location, "F", femaleResult);
            log.info("女性穿搭建议已缓存, city={}", location);

            String result = "M".equals(gender) ? maleResult : femaleResult;
            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("AI穿搭建议生成失败: {}", e.getMessage());
        }

        redisCacheUtil.setOutfit(location, "M", DEFAULT_OUTFIT);
        redisCacheUtil.setOutfit(location, "F", DEFAULT_OUTFIT);
        return CommonResult.success("M".equals(gender) ? DEFAULT_OUTFIT : DEFAULT_OUTFIT);
    }

    /**
     * 异步预加载穿搭建议（后台执行，不阻塞）
     */
    @Async
    public void preloadOutfitSuggestion(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }

        try {
            log.info("开始异步预加载穿搭建议, userId={}", userId);
            getOutfitSuggestion(userId, null);
            log.info("穿搭建议预加载完成, userId={}", userId);
        } catch (Exception e) {
            log.error("穿搭建议预加载失败, userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public CommonResult<?> sendMorningPush(Long userId) {
        return CommonResult.success("推送已发送");
    }
}