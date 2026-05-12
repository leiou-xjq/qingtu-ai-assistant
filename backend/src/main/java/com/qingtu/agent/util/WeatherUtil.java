package com.qingtu.agent.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.qingtu.agent.config.WeatherConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 天气API调用工具类 - 使用心知天气(Seniverse)API
 */
@Slf4j
@Component
public class WeatherUtil {

    private final WeatherConfig weatherConfig;
    private final RestTemplate restTemplate;

    public WeatherUtil(WeatherConfig weatherConfig) {
        this.weatherConfig = weatherConfig;
        this.restTemplate = new RestTemplate();
    }

    public WeatherInfo getCurrentWeather(String location) {
        if (location == null || location.isEmpty()) {
            location = "北京";
        }

        try {
            // 心知天气API
            String url = String.format(
                "https://api.seniverse.com/v3/weather/now.json?key=%s&location=%s&language=zh-Hans",
                weatherConfig.getApiKey(), location
            );

            log.info("调用心知天气API: {}", url);

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            String responseBody = response.getBody();
            log.info("心知天气返回: {}", responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("心知天气返回数据为空");
                return createDefaultWeather(location);
            }

            JSONObject json = JSONObject.parseObject(responseBody);
            
            // 检查返回码
            JSONArray results = json.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                log.warn("心知天气返回数据格式错误");
                return createDefaultWeather(location);
            }

            JSONObject result = results.getJSONObject(0);
            JSONObject now = result.getJSONObject("now");
            JSONObject loc = result.getJSONObject("location");

            if (now == null) {
                log.warn("心知天气返回数据格式错误");
                return createDefaultWeather(location);
            }

            // 解析天气数据
            WeatherInfo info = new WeatherInfo();
            info.setCityName(loc.getString("name"));
            info.setTemp(getStringOrDefault(now, "temperature", "25"));
            info.setFeelsLike(getStringOrDefault(now, "temperature", "25"));
            info.setText(getStringOrDefault(now, "text", "晴"));
            info.setHumidity(getStringOrDefault(now, "humidity", "--"));
            info.setWindDir(getStringOrDefault(now, "wind_direction", "") + "风");
            info.setWindScale(getStringOrDefault(now, "wind_scale", "") + "级");
            info.setUvIndex(getUvIndex(getStringOrDefault(now, "temperature", "25")));
            info.setUpdateTime(getStringOrDefault(now, "last_update", "刚刚"));

            log.info("天气数据解析成功: {}", info.getWeatherSummary());
            return info;

        } catch (Exception e) {
            log.error("获取天气异常: {}", e.getMessage());
            return createDefaultWeather(location);
        }
    }

    private String getStringOrDefault(JSONObject json, String key, String defaultValue) {
        try {
            if (json == null || !json.containsKey(key) || json.get(key) == null) {
                return defaultValue;
            }
            String value = json.getString(key);
            return (value == null || value.isEmpty()) ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getUvIndex(String temp) {
        try {
            int t = Integer.parseInt(temp);
            if (t < 10) return "弱";
            if (t < 20) return "中等";
            if (t < 30) return "强";
            return "很强";
        } catch (Exception e) {
            return "中等";
        }
    }

    private WeatherInfo createDefaultWeather(String location) {
        WeatherInfo info = new WeatherInfo();
        info.setCityName(location);
        info.setTemp("25");
        info.setFeelsLike("26");
        info.setText("多云");
        info.setWindDir("东南风");
        info.setWindScale("3级");
        info.setHumidity("60");
        info.setUvIndex("中等");
        info.setUpdateTime("刚刚");
        return info;
    }

    public JSONArray getForecast(String location, int days) {
        return new JSONArray();
    }

    public List<DailyForecast> getDailyForecast(String location, int days) {
        List<DailyForecast> forecasts = new ArrayList<>();
        if (location == null || location.isEmpty()) {
            location = "北京";
        }
        if (days < 1) days = 3;
        if (days > 15) days = 15;

        try {
            String url = String.format(
                "https://api.seniverse.com/v3/weather/daily.json?key=%s&location=%s&language=zh-Hans&days=%d",
                weatherConfig.getApiKey(), location, days
            );

            log.info("调用心知天气预报API: {}", url);

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String responseBody = response.getBody();
            log.info("心知天气预报返回: {}", responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                return forecasts;
            }

            JSONObject json = JSONObject.parseObject(responseBody);
            JSONArray results = json.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return forecasts;
            }

            JSONObject result = results.getJSONObject(0);
            JSONArray daily = result.getJSONArray("daily");
            if (daily == null || daily.isEmpty()) {
                return forecasts;
            }

            for (int i = 0; i < daily.size(); i++) {
                JSONObject day = daily.getJSONObject(i);
                DailyForecast forecast = new DailyForecast();
                forecast.setDate(day.getString("date"));
                forecast.setTextDay(day.getString("text_day"));
                forecast.setTextNight(day.getString("text_night"));
                forecast.setTempHigh(day.getString("temp_high"));
                forecast.setTempLow(day.getString("temp_low"));
                forecast.setWindDir(day.getString("wind_direction"));
                forecast.setWindScale(day.getString("wind_scale"));
                forecast.setHumidity(day.getString("humidity"));
                forecast.setUvIndex(getUvIndexFromCode(day.getString("uv_index")));
                forecasts.add(forecast);
            }

        } catch (Exception e) {
            log.error("获取天气预报异常: {}", e.getMessage());
        }
        return forecasts;
    }

    private String getUvIndexFromCode(String code) {
        try {
            if (code == null || code.isEmpty()) return "中等";
            int level = Integer.parseInt(code);
            if (level <= 2) return "较弱";
            if (level == 3) return "中等";
            if (level == 4) return "较强";
            if (level == 5) return "强";
            return "很强";
        } catch (Exception e) {
            return "中等";
        }
    }

    public static class DailyForecast {
        private String date;
        private String textDay;
        private String textNight;
        private String tempHigh;
        private String tempLow;
        private String windDir;
        private String windScale;
        private String humidity;
        private String uvIndex;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTextDay() { return textDay; }
        public void setTextDay(String textDay) { this.textDay = textDay; }
        public String getTextNight() { return textNight; }
        public void setTextNight(String textNight) { this.textNight = textNight; }
        public String getTempHigh() { return tempHigh; }
        public void setTempHigh(String tempHigh) { this.tempHigh = tempHigh; }
        public String getTempLow() { return tempLow; }
        public void setTempLow(String tempLow) { this.tempLow = tempLow; }
        public String getWindDir() { return windDir; }
        public void setWindDir(String windDir) { this.windDir = windDir; }
        public String getWindScale() { return windScale; }
        public void setWindScale(String windScale) { this.windScale = windScale; }
        public String getHumidity() { return humidity; }
        public void setHumidity(String humidity) { this.humidity = humidity; }
        public String getUvIndex() { return uvIndex; }
        public void setUvIndex(String uvIndex) { this.uvIndex = uvIndex; }

        public String getDateDisplay() {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(this.date);
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("M月d日");
                String monthDay = date.format(formatter);
                int dayOfWeek = date.getDayOfWeek().getValue();
                String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                return monthDay + "（" + weekDays[dayOfWeek - 1] + "）";
            } catch (Exception e) {
                return this.date;
            }
        }

        public String getWeatherEmoji() {
            if (textDay == null) return "🌤️";
            return switch (textDay) {
                case "晴" -> "☀️";
                case "多云" -> "⛅";
                case "阴" -> "☁️";
                case "小雨" -> "🌧️";
                case "中雨" -> "🌧️";
                case "大雨" -> "⛈️";
                case "雷阵雨" -> "⛈️";
                case "小雪" -> "🌨️";
                case "中雪" -> "❄️";
                case "大雪" -> "❄️";
                case "雾" -> "🌫️";
                case "霾" -> "🌫️";
                case "沙尘暴" -> "🌪️";
                default -> "🌤️";
            };
        }
    }

    public static class WeatherInfo {
        private String cityName;
        private String temp;
        private String feelsLike;
        private String text;
        private String windDir;
        private String windScale;
        private String humidity;
        private String precip;
        private String uvIndex;
        private String updateTime;

        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }
        public String getTemp() { return temp; }
        public void setTemp(String temp) { this.temp = temp; }
        public String getFeelsLike() { return feelsLike; }
        public void setFeelsLike(String feelsLike) { this.feelsLike = feelsLike; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getWindDir() { return windDir; }
        public void setWindDir(String windDir) { this.windDir = windDir; }
        public String getWindScale() { return windScale; }
        public void setWindScale(String windScale) { this.windScale = windScale; }
        public String getHumidity() { return humidity; }
        public void setHumidity(String humidity) { this.humidity = humidity; }
        public String getPrecip() { return precip; }
        public void setPrecip(String precip) { this.precip = precip; }
        public String getUvIndex() { return uvIndex; }
        public void setUvIndex(String uvIndex) { this.uvIndex = uvIndex; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

        public String getWeatherSummary() {
            return String.format("%s %s°C，%s%s，湿度%s%%，紫外线%s",
                    cityName, temp, windDir, windScale, humidity, uvIndex);
        }
    }
}