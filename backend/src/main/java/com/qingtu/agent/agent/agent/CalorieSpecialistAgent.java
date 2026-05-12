package com.qingtu.agent.agent.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.entity.po.CalorieIntake;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.CalorieIntakeMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalorieSpecialistAgent {

    private final CalorieIntakeMapper calorieIntakeMapper;
    private final UserHealthMapper userHealthMapper;
    private final DashScopeConfig dashScopeConfig;
    private final RestTemplate restTemplate;

    private static final Map<String, String> MEAL_TYPE_MAP = new HashMap<>();

    static {
        MEAL_TYPE_MAP.put("早餐", "breakfast");
        MEAL_TYPE_MAP.put("早饭", "breakfast");
        MEAL_TYPE_MAP.put("breakfast", "breakfast");
        MEAL_TYPE_MAP.put("午餐", "lunch");
        MEAL_TYPE_MAP.put("午饭", "lunch");
        MEAL_TYPE_MAP.put("中餐", "lunch");
        MEAL_TYPE_MAP.put("lunch", "lunch");
        MEAL_TYPE_MAP.put("晚餐", "dinner");
        MEAL_TYPE_MAP.put("晚饭", "dinner");
        MEAL_TYPE_MAP.put("dinner", "dinner");
        MEAL_TYPE_MAP.put("加餐", "snack");
        MEAL_TYPE_MAP.put("零食", "snack");
        MEAL_TYPE_MAP.put("snack", "snack");
    }

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();

        try {
            return switch (action.toLowerCase()) {
                case "create", "record" -> createCalorieRecord(taskId, context, params, correlationId);
                case "query" -> queryTodayCalorie(taskId, context, params, correlationId);
                default -> ResultMessage.failure(taskId, "calorie", action, "未知动作: " + action, correlationId, context.getUserId());
            };
        } catch (Exception e) {
            log.error("卡路里记录执行失败", e);
            return ResultMessage.failure(taskId, "calorie", action, e.getMessage(), correlationId, context.getUserId());
        }
    }

    private ResultMessage createCalorieRecord(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        String foodInput = extractFoodInput(params);
        String mealType = extractMealType(params);
        Integer calories = extractCalories(params, foodInput);

        if (foodInput == null || foodInput.isBlank()) {
            return ResultMessage.failure(taskId, "calorie", "create", "请告诉我你吃了什么食物", correlationId, context.getUserId());
        }

        CalorieIntake intake = new CalorieIntake();
        intake.setUserId(context.getUserId());
        intake.setMealType(mealType);
        intake.setFoodInput(foodInput);
        intake.setEstimatedCalories(calories);
        intake.setRecordDate(LocalDate.now());
        intake.setCreateTime(LocalDateTime.now());

        calorieIntakeMapper.insert(intake);

        log.info("卡路里记录成功: userId={}, intakeId={}, food={}, calories={}",
                context.getUserId(), intake.getId(), foodInput, calories);

        UserHealth health = getUserHealth(context.getUserId());
        int todayTotal = calculateTodayTotal(context.getUserId());

        return ResultMessage.success(taskId, "calorie", "create",
                Map.of(
                        "intakeId", intake.getId(),
                        "food", foodInput,
                        "calories", calories,
                        "mealType", mealType,
                        "todayTotal", todayTotal,
                        "dailyTarget", health != null ? health.getDailyCalories() : 2000,
                        "message", buildSuccessMessage(foodInput, calories, todayTotal, health)
                ),
                correlationId, context.getUserId());
    }

    private ResultMessage queryTodayCalorie(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        UserHealth health = getUserHealth(context.getUserId());
        int todayTotal = calculateTodayTotal(context.getUserId());

        List<CalorieIntake> todayIntakes = calorieIntakeMapper.selectList(
                new LambdaQueryWrapper<CalorieIntake>()
                        .eq(CalorieIntake::getUserId, context.getUserId())
                        .eq(CalorieIntake::getRecordDate, LocalDate.now())
                        .eq(CalorieIntake::getDeleted, 0)
                        .orderByDesc(CalorieIntake::getCreateTime)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("todayTotal", todayTotal);
        result.put("dailyTarget", health != null ? health.getDailyCalories() : 2000);
        result.put("percentage", health != null && health.getDailyCalories() > 0
                ? (todayTotal * 100 / health.getDailyCalories()) : 0);
        result.put("records", todayIntakes.stream().map(this::toRecordMap).toList());
        result.put("message", buildQueryMessage(todayTotal, health, todayIntakes.size()));

        return ResultMessage.success(taskId, "calorie", "query", result, correlationId, context.getUserId());
    }

    private String extractFoodInput(Map<String, Object> params) {
        String[] keys = {"food", "foodInput", "description", "content", "食物", "吃了", "吃的"};
        for (String key : keys) {
            Object val = params.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString().trim();
            }
        }
        return null;
    }

    private String extractMealType(Map<String, Object> params) {
        Object typeObj = params.get("meal");
        if (typeObj != null && !typeObj.toString().isBlank()) {
            return mapMealType(typeObj.toString());
        }

        typeObj = params.get("mealType");
        if (typeObj != null && !typeObj.toString().isBlank()) {
            return mapMealType(typeObj.toString());
        }

        Object descObj = params.getOrDefault("description", params.getOrDefault("content", ""));
        String desc = descObj.toString();

        for (Map.Entry<String, String> entry : MEAL_TYPE_MAP.entrySet()) {
            if (desc.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        int hour = LocalDateTime.now().getHour();
        if (hour >= 5 && hour < 10) return "breakfast";
        if (hour >= 10 && hour < 14) return "lunch";
        if (hour >= 18 && hour < 21) return "dinner";
        return "snack";
    }

    private String mapMealType(String type) {
        String lower = type.toLowerCase();
        if (lower.contains("breakfast") || lower.contains("早餐") || lower.contains("早饭")) {
            return "breakfast";
        }
        if (lower.contains("lunch") || lower.contains("午餐") || lower.contains("午饭") || lower.contains("中餐")) {
            return "lunch";
        }
        if (lower.contains("dinner") || lower.contains("晚餐") || lower.contains("晚饭")) {
            return "dinner";
        }
        if (lower.contains("snack") || lower.contains("加餐") || lower.contains("零食")) {
            return "snack";
        }
        return "lunch";
    }

    private Integer extractCalories(Map<String, Object> params, String foodInput) {
        Object caloriesObj = params.get("calories");
        if (caloriesObj instanceof Number) {
            return ((Number) caloriesObj).intValue();
        }
        if (caloriesObj != null) {
            try {
                return Integer.parseInt(caloriesObj.toString().replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {}
        }

        return estimateCaloriesWithLLM(foodInput);
    }

    private Integer estimateCaloriesWithLLM(String foodInput) {
        try {
            String prompt = "你是食物卡路里估算助手。根据用户输入的食物描述，估算这餐的大致卡路里摄入量（单位：大卡）。\n\n" +
                    "食物描述：" + foodInput + "\n\n" +
                    "要求：\n" +
                    "1. 只输出一个整数数字，不要任何解释\n" +
                    "2. 估算要基于常见份量（如一碗米饭约200卡，一个鸡腿约250卡）\n" +
                    "3. 如果描述中包含份量信息，按实际份量估算\n\n" +
                    "输出示例：350";

            String url = dashScopeConfig.getBaseUrl() + "/chat/completions";

            Map<String, Object> body = new HashMap<>();
            body.put("model", dashScopeConfig.getModel());
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("enable_search", false);
            body.put("temperature", 0.3);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            if (response != null) {
                String content = extractContentFromResponse(response);
                if (content != null) {
                    String numStr = content.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        return Math.min(Integer.parseInt(numStr), 3000);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LLM估算卡路里失败，使用默认值: {}", e.getMessage());
        }
        return 300;
    }

    private String extractContentFromResponse(String response) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(response);
            var choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            log.warn("解析LLM响应失败: {}", e.getMessage());
        }
        return null;
    }

    private UserHealth getUserHealth(Long userId) {
        return userHealthMapper.selectOne(
                new LambdaQueryWrapper<UserHealth>()
                        .eq(UserHealth::getUserId, userId)
                        .eq(UserHealth::getDeleted, 0)
        );
    }

    private int calculateTodayTotal(Long userId) {
        List<CalorieIntake> intakes = calorieIntakeMapper.selectList(
                new LambdaQueryWrapper<CalorieIntake>()
                        .eq(CalorieIntake::getUserId, userId)
                        .eq(CalorieIntake::getRecordDate, LocalDate.now())
                        .eq(CalorieIntake::getDeleted, 0)
        );
        return intakes.stream().mapToInt(CalorieIntake::getEstimatedCalories).sum();
    }

    private Map<String, Object> toRecordMap(CalorieIntake intake) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", intake.getId());
        map.put("food", intake.getFoodInput());
        map.put("calories", intake.getEstimatedCalories());
        map.put("mealType", intake.getMealType());
        map.put("time", intake.getCreateTime());
        return map;
    }

    private String buildSuccessMessage(String food, int calories, int todayTotal, UserHealth health) {
        int dailyTarget = health != null && health.getDailyCalories() != null ? health.getDailyCalories() : 2000;
        int percentage = dailyTarget > 0 ? (todayTotal * 100 / dailyTarget) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("已记录：").append(food).append(" (").append(calories).append("卡)\n");
        sb.append("今日摄入：").append(todayTotal).append("/").append(dailyTarget).append("卡 (").append(percentage).append("%)");

        if (health != null) {
            BigDecimal height = health.getHeight();
            BigDecimal weight = health.getWeight();
            if (height != null && weight != null && height.compareTo(BigDecimal.ZERO) > 0) {
                double bmi = weight.doubleValue() / Math.pow(height.doubleValue() / 100, 2);
                if (percentage > 100) {
                    sb.append("\n💡 今日已超标").append(percentage - 100).append("%，建议适量减少晚餐");
                } else if (percentage < 50) {
                    sb.append("\n💡 建议适当补充营养");
                } else {
                    sb.append("\n💡 今日饮食控制良好");
                }
            }
        }

        return sb.toString();
    }

    private String buildQueryMessage(int todayTotal, UserHealth health, int recordCount) {
        int dailyTarget = health != null && health.getDailyCalories() != null ? health.getDailyCalories() : 2000;
        int percentage = dailyTarget > 0 ? (todayTotal * 100 / dailyTarget) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("今日已记录").append(recordCount).append("条，共").append(todayTotal).append("卡\n");
        sb.append("目标：").append(dailyTarget).append("卡，达成").append(percentage).append("%");

        if (percentage > 100) {
            sb.append("\n⚠️ 已超标").append(percentage - 100).append("%，注意控制");
        } else if (percentage >= 80) {
            sb.append("\n✅ 接近目标，保持均衡");
        } else {
            sb.append("\n💡 距离目标还需").append(dailyTarget - todayTotal).append("卡");
        }

        return sb.toString();
    }
}