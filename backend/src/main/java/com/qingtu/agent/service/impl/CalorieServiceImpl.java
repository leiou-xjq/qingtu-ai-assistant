package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.entity.po.CalorieIntake;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.CalorieIntakeMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.service.CalorieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalorieServiceImpl implements CalorieService {

    private final CalorieIntakeMapper calorieIntakeMapper;
    private final UserHealthMapper userHealthMapper;
    private final QingTuAgent agent;
    private final DashScopeConfig dashScopeConfig;

    @Override
    public CommonResult<?> recordIntake(Long userId, String mealType, String foodInput) {
        try {
            // 检查今天是否已记录过该餐
            Long existingCount = calorieIntakeMapper.selectCount(new LambdaQueryWrapper<CalorieIntake>()
                    .eq(CalorieIntake::getUserId, userId)
                    .eq(CalorieIntake::getMealType, mealType)
                    .eq(CalorieIntake::getRecordDate, LocalDate.now())
                    .eq(CalorieIntake::getDeleted, 0));

            if (existingCount > 0) {
                String mealName = mealType.equals("breakfast") ? "早餐" : mealType.equals("lunch") ? "午餐" : "晚餐";
                return CommonResult.fail("今天已记录过" + mealName + "，今日无法重复记录");
            }

            // 先保存记录（使用默认热量500）
            CalorieIntake intake = new CalorieIntake();
            intake.setUserId(userId);
            intake.setMealType(mealType);
            intake.setFoodInput(foodInput);
            intake.setEstimatedCalories(500);
            intake.setRecordDate(LocalDate.now());
            calorieIntakeMapper.insert(intake);

            // 异步计算热量并更新
            final Long intakeId = intake.getId();
            final String food = foodInput;
            final Long uid = userId;
            final String mType = mealType;

            // 异步计算
            new Thread(() -> {
                try {
                    int calories = estimateCalories(food);
                    CalorieIntake existing = calorieIntakeMapper.selectById(intakeId);
                    if (existing != null && existing.getEstimatedCalories() == 500) {
                        existing.setEstimatedCalories(calories);
                        calorieIntakeMapper.updateById(existing);
                        log.info("热量计算完成: food={}, calories={}", food, calories);
                    }
                } catch (Exception e) {
                    log.error("异步热量计算失败: food={}, error={}", food, e.getMessage());
                }
            }).start();

            Map<String, Object> result = new HashMap<>();
            result.put("mealType", mealType);
            result.put("foodInput", foodInput);
            result.put("estimatedCalories", 500);
            result.put("pending", true);

            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("记录失败", e);
            return CommonResult.fail("记录失败: " + e.getMessage());
        }
    }

    @Override
    public CommonResult<?> getTodayProgress(Long userId) {
        // 获取今日记录
        List<CalorieIntake> todayIntakes = calorieIntakeMapper.selectList(new LambdaQueryWrapper<CalorieIntake>()
                .eq(CalorieIntake::getUserId, userId)
                .eq(CalorieIntake::getRecordDate, LocalDate.now())
                .eq(CalorieIntake::getDeleted, 0));

        // 计算已摄入热量
        int consumedCalories = todayIntakes.stream()
                .mapToInt(CalorieIntake::getEstimatedCalories)
                .sum();

        // 获取用户每日目标热量
        int dailyTarget = 1800; // 默认值
        UserHealth health = userHealthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0));
        if (health != null && health.getDailyCalories() != null) {
            dailyTarget = health.getDailyCalories();
        }

        // 计算进度百分比
        int progressPercent = dailyTarget > 0 ? (consumedCalories * 100 / dailyTarget) : 0;
        progressPercent = Math.min(progressPercent, 100);

        // 构建结果
        Map<String, Object> result = new HashMap<>();
        result.put("consumedCalories", consumedCalories);
        result.put("dailyTarget", dailyTarget);
        result.put("progressPercent", progressPercent);
        result.put("remainingCalories", Math.max(dailyTarget - consumedCalories, 0));

        // 各餐记录状态
        Map<String, Map<String, Object>> meals = new HashMap<>();
        String[] mealTypes = {"breakfast", "lunch", "dinner"};
        for (String mt : mealTypes) {
            Map<String, Object> mealInfo = new HashMap<>();
            mealInfo.put("recorded", false);
            mealInfo.put("calories", 0);
            meals.put(mt, mealInfo);
        }
        for (CalorieIntake intake : todayIntakes) {
            String meal = intake.getMealType();
            if (meals.containsKey(meal)) {
                Map<String, Object> mealInfo = meals.get(meal);
                mealInfo.put("recorded", true);
                mealInfo.put("calories", intake.getEstimatedCalories());
                mealInfo.put("foodInput", intake.getFoodInput());
            }
        }
        result.put("meals", meals);

        // 分类显示（兼容旧版）
        Map<String, Integer> byMeal = new HashMap<>();
        for (CalorieIntake intake : todayIntakes) {
            String meal = intake.getMealType();
            byMeal.put(meal, byMeal.getOrDefault(meal, 0) + intake.getEstimatedCalories());
        }
        result.put("byMeal", byMeal);

        // 详细记录
        List<Map<String, Object>> details = new ArrayList<>();
        for (CalorieIntake intake : todayIntakes) {
            Map<String, Object> item = new HashMap<>();
            item.put("mealType", intake.getMealType());
            item.put("foodInput", intake.getFoodInput());
            item.put("calories", intake.getEstimatedCalories());
            item.put("time", intake.getCreateTime().toString());
            details.add(item);
        }
        result.put("details", details);

        return CommonResult.success(result);
    }

    private int estimateCalories(String foodInput) {
        try {
            String prompt = "请根据以下食物描述，估算其热量（千卡）。\n" +
                    "食物：" + foodInput + "\n" +
                    "请直接返回一个数字（整数），如：500。不要有任何其他文字。";

            String response;
            // 优先使用 agent（AiClient），失败再用 DashScope
            try {
                response = agent.chat(prompt);
            } catch (Exception e) {
                log.warn("Agent调用失败，尝试备用方案: {}", e.getMessage());
                if (dashScopeConfig.isConfigured()) {
                    try {
                        response = callDashScopeAI(prompt);
                    } catch (Exception ex) {
                        log.error("备用AI调用也失败: {}", ex.getMessage());
                        return 500; // 默认值
                    }
                } else {
                    return 500; // 默认值
                }
            }

            // 提取数字
            String numbers = response.replaceAll("[^0-9]", "");
            if (!numbers.isEmpty()) {
                return Integer.parseInt(numbers);
            }
        } catch (Exception e) {
            log.error("AI估算热量失败，使用默认值", e);
        }
        return 500; // 默认值
    }

    private String callDashScopeAI(String prompt) {
        String url = dashScopeConfig.getBaseUrl() + "/compatible-mode/v1/services/aigc/text-generation/generation";

        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        input.put("messages", messages);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", dashScopeConfig.getModel());
        requestBody.put("input", input);
        Map<String, Object> params = new HashMap<>();
        params.put("result_format", "message");
        requestBody.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(url, entity, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode output = root.path("output");
            JsonNode choices = output.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            return root.path("output").path("text").asText();
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            throw new RuntimeException("AI响应解析失败");
        }
    }
}