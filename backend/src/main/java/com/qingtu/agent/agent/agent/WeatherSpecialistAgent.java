package com.qingtu.agent.agent.agent;

import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.config.WeatherConfig;
import com.qingtu.agent.util.WeatherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 天气专家 Agent
 */
@Slf4j
@Component
public class WeatherSpecialistAgent {

    private final WeatherConfig weatherConfig;

    public WeatherSpecialistAgent(WeatherConfig weatherConfig) {
        this.weatherConfig = weatherConfig;
    }

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String taskId = java.util.UUID.randomUUID().toString();

        try {
            String city = extractCity(params, context);
            WeatherUtil weatherUtil = new WeatherUtil(weatherConfig);

            if ("forecast".equalsIgnoreCase(action)) {
                int dayOffset = extractDayOffset(params);
                int days = dayOffset > 0 ? dayOffset + 2 : 3;
                var forecasts = weatherUtil.getDailyForecast(city, days);
                return ResultMessage.success(taskId, "weather", action,
                        Map.of(
                                "city", city,
                                "forecasts", forecasts,
                                "dayOffset", dayOffset
                        ),
                        params.getOrDefault("_correlationId", "").toString(),
                        context.getUserId());
            } else {
                WeatherUtil.WeatherInfo info = weatherUtil.getCurrentWeather(city);
                return ResultMessage.success(taskId, "weather", action,
                        Map.of(
                                "city", city,
                                "temp", info.getTemp(),
                                "text", info.getText(),
                                "feelsLike", info.getFeelsLike(),
                                "humidity", info.getHumidity(),
                                "wind", info.getWindDir() + info.getWindScale(),
                                "summary", info.getWeatherSummary()
                        ),
                        params.getOrDefault("_correlationId", "").toString(),
                        context.getUserId());
            }
        } catch (Exception e) {
            log.error("天气查询失败", e);
            return ResultMessage.failure(taskId, "weather", action,
                    e.getMessage(),
                    params.getOrDefault("_correlationId", "").toString(),
                    context.getUserId());
        }
    }

    private String extractCity(Map<String, Object> params, UserContext context) {
        Object cityObj = params.getOrDefault("city", params.getOrDefault("_city", null));
        if (cityObj != null && !cityObj.toString().isBlank()) {
            return cityObj.toString();
        }
        return context.getCity() != null ? context.getCity() : "北京";
    }

    private int extractDayOffset(Map<String, Object> params) {
        Object dayOffsetObj = params.get("dayOffset");
        if (dayOffsetObj != null) {
            try {
                return Integer.parseInt(dayOffsetObj.toString());
            } catch (NumberFormatException ignored) {}
        }

        Object dayObj = params.get("day");
        if (dayObj != null) {
            String day = dayObj.toString().toLowerCase();
            if (day.contains("明天") || day.contains("tomorrow")) return 1;
            if (day.contains("后天") || day.contains("后天")) return 2;
            if (day.contains("大后天") || day.contains("大后天")) return 3;
            if (day.contains("一周") || day.contains("周") || day.contains("week")) return 7;
        }

        Object textObj = params.get("message");
        if (textObj != null) {
            String text = textObj.toString().toLowerCase();
            if (text.contains("明天") || text.contains("tomorrow")) return 1;
            if (text.contains("后天") || text.contains("大后天")) return 2;
            if (text.contains("大后天")) return 3;
            if (text.contains("一周") || text.contains("周")) return 7;
        }

        return 0;
    }
}