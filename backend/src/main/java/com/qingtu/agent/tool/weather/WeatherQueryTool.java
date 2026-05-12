package com.qingtu.agent.tool.weather;

import com.qingtu.agent.config.WeatherConfig;
import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolExecutor;
import com.qingtu.agent.util.WeatherUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 天气查询工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherQueryTool implements ToolExecutor {

    private final WeatherConfig weatherConfig;

    @Override
    public String getName() {
        return "weather_query";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的实时天气和天气预报信息";
    }

    @Override
    public String getCategory() {
        return "weather";
    }

    @Override
    public ToolDefinition.ExecuteResult execute(Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        try {
            String city = (String) arguments.getOrDefault("city", "北京");
            String type = (String) arguments.getOrDefault("type", "current");

            WeatherUtil weatherUtil = new WeatherUtil(weatherConfig);

            if ("forecast".equals(type)) {
                var forecasts = weatherUtil.getForecast(city, 3);
                return ToolDefinition.ExecuteResult.success(Map.of("city", city, "forecast", forecasts));
            } else {
                WeatherUtil.WeatherInfo info = weatherUtil.getCurrentWeather(city);
                return ToolDefinition.ExecuteResult.success(Map.of(
                        "city", city,
                        "temp", info.getTemp(),
                        "text", info.getText(),
                        "feelsLike", info.getFeelsLike(),
                        "humidity", info.getHumidity(),
                        "wind", info.getWindDir() + info.getWindScale(),
                        "summary", info.getWeatherSummary()
                ));
            }
        } catch (Exception e) {
            log.error("天气查询失败", e);
            return ToolDefinition.ExecuteResult.error("天气查询失败: " + e.getMessage());
        }
    }

    @Override
    public long getTimeoutMs() {
        return 10000;
    }
}
