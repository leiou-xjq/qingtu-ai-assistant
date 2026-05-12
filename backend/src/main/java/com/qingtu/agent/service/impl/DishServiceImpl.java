package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.entity.po.CanteenDish;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.CanteenDishMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.rag.RagServiceCore;
import com.qingtu.agent.service.DishService;
import com.qingtu.agent.util.RedisCacheUtil;
import com.qingtu.agent.util.WeatherUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final CanteenDishMapper dishMapper;
    private final UserHealthMapper userHealthMapper;
    private final WeatherUtil weatherUtil;
    private final RagServiceCore ragService;
    private final QingTuAgent agent;
    private final DashScopeConfig dashScopeConfig;
    private final RedisCacheUtil redisCacheUtil;

    @Override
    @Cacheable(value = "dishes", key = "#type + '_' + #category + '_' + #page + '_' + #size")
    public CommonResult<?> listDishes(String type, String category, int page, int size) {
        LambdaQueryWrapper<CanteenDish> wrapper = new LambdaQueryWrapper<CanteenDish>()
                .eq(CanteenDish::getStatus, 1)
                .eq(CanteenDish::getDeleted, 0);

        if (type != null && !type.isEmpty()) {
            wrapper.eq(CanteenDish::getType, type);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(CanteenDish::getCategory, category);
        }

        Page<CanteenDish> dishPage = new Page<>(page, size);
        dishMapper.selectPage(dishPage, wrapper);

        return CommonResult.success(dishPage);
    }

    @Override
    public CommonResult<?> getDishById(Long dishId) {
        CanteenDish dish = dishMapper.selectById(dishId);
        if (dish == null || dish.getDeleted() == 1) {
            return CommonResult.fail(ResultCode.DISH_NOT_FOUND);
        }
        return CommonResult.success(dish);
    }

    @Override
    public CommonResult<?> recommendDishes(Long userId, String mealType) {
        String ragContext = ragService.retrieveCommonContext(mealType + "饮食推荐", 3);
        
        String mealName;
        if ("breakfast".equals(mealType)) {
            mealName = "早餐";
        } else if ("lunch".equals(mealType)) {
            mealName = "午餐";
        } else if ("dinner".equals(mealType)) {
            mealName = "晚餐";
        } else {
            mealName = "用餐";
        }
        String instruction = "请为用户推荐适合" + mealName + "的菜品，考虑营养均衡、价格实惠、符合大学生口味。";

        try {
            String prompt = instruction + "请用JSON格式返回推荐菜品列表。";
            String response = agent.chat(prompt);
            Map<String, Object> result = new HashMap<>();
            result.put("aiRecommendation", response);
            result.put("mealType", mealType);
            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("AI推荐失败", e);
            List<CanteenDish> dishes = dishMapper.selectList(
                    new LambdaQueryWrapper<CanteenDish>()
                            .eq(CanteenDish::getType, mealType)
                            .eq(CanteenDish::getStatus, 1)
                            .eq(CanteenDish::getDeleted, 0)
                            .last("LIMIT 5")
            );
            Map<String, Object> result = new HashMap<>();
            result.put("recommendations", dishes);
            return CommonResult.success(result);
        }
    }

    @Override
    public CommonResult<?> getTodayRecommendation(Long userId) {
        // 获取用户健康数据
        UserHealth health = userHealthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0));

        // 检查Redis缓存
        String cached = redisCacheUtil.getRecipe(userId);
        if (cached != null && !cached.isEmpty()) {
            log.info("使用Redis缓存的今日饮食推荐, userId={}", userId);
            return CommonResult.success(cached);
        }

        // 生成AI推荐
        try {
            String aiRecommendation = generateAIRecommendation(health);
            redisCacheUtil.setRecipeWithTime(userId, aiRecommendation, java.time.LocalDateTime.now());
            return CommonResult.success(aiRecommendation);
        } catch (Exception e) {
            log.error("AI推荐失败，使用备用方案", e);
            return CommonResult.success(generateFallbackRecommendation(health));
        }
    }

    @Override
    public void refreshTodayRecommendationAsync(Long userId) {
        new Thread(() -> {
            try {
                log.info("开始异步刷新今日饮食推荐...");
                UserHealth health = userHealthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                        .eq(UserHealth::getUserId, userId)
                        .eq(UserHealth::getDeleted, 0));

                String aiRecommendation = generateAIRecommendation(health);
                redisCacheUtil.setRecipeWithTime(userId, aiRecommendation, java.time.LocalDateTime.now());
                log.info("今日饮食推荐刷新完成");
            } catch (Exception e) {
                log.error("刷新今日推荐失败: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * 异步预加载今日推荐（可选调用）
     * 用户登录后可调用此方法后台预加载
     */
    @Async
    public CompletableFuture<Void> preloadTodayRecommendation(Long userId) {
        try {
            log.info("开始异步预加载今日饮食推荐...");
            UserHealth health = userHealthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                    .eq(UserHealth::getUserId, userId)
                    .eq(UserHealth::getDeleted, 0));

            String aiRecommendation = generateAIRecommendation(health);
            redisCacheUtil.setRecipeWithTime(userId, aiRecommendation, java.time.LocalDateTime.now());
            log.info("今日饮食推荐预加载完成");
        } catch (Exception e) {
            log.error("预加载今日推荐失败: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private String generateAIRecommendation(UserHealth health) throws Exception {
        // 构建用户健康信息
        StringBuilder userInfo = new StringBuilder();
        userInfo.append("用户信息：\n");

        if (health != null) {
            BigDecimal bmi = health.getBmi();
            if (bmi != null) {
                String bmiLevel;
                if (bmi.doubleValue() < 18.5) {
                    bmiLevel = "偏瘦";
                } else if (bmi.doubleValue() < 24) {
                    bmiLevel = "正常";
                } else if (bmi.doubleValue() < 28) {
                    bmiLevel = "偏胖";
                } else {
                    bmiLevel = "肥胖";
                }
                userInfo.append("- BMI：").append(bmi).append("（").append(bmiLevel).append("）\n");
            }

            if (health.getGender() != null) {
                userInfo.append("- 性别：").append(health.getGender().equals("M") ? "男" : "女").append("\n");
            }
            if (health.getAge() != null) {
                userInfo.append("- 年龄：").append(health.getAge()).append("岁\n");
            }
            if (health.getActivityLevel() != null) {
                String actLevel;
                double act = health.getActivityLevel().doubleValue();
                if (act < 1.2) actLevel = "久坐";
                else if (act < 1.4) actLevel = "轻度活动";
                else if (act < 1.6) actLevel = "中度活动";
                else actLevel = "高度活动";
                userInfo.append("- 活动水平：").append(actLevel).append("\n");
            }
            if (health.getTastePreference() != null) {
                userInfo.append("- 口味偏好：").append(health.getTastePreference()).append("\n");
            }
            if (health.getTabooFood() != null) {
                userInfo.append("- 忌口：").append(health.getTabooFood()).append("\n");
            }
        } else {
            userInfo.append("- （未完善健康数据，按标准推荐）\n");
        }

String prompt = userInfo + """

请根据以上用户信息，推荐今日三餐食谱。

要求：
1. 考虑用户的BMI体型，推荐合适热量的菜品
2. 早餐400-600大卡，午餐600-800大卡，晚餐400-600大卡
3. 营养均衡：蛋白质、碳水、脂肪合理配比
4. 每餐包含：主食、蛋白质来源、蔬菜、水果（如有）
5. 如果BMI偏胖，建议低脂高蛋白；如偏瘦，建议营养丰富
6. 忌口食物需要避开

请直接返回以下JSON格式，不要有任何其他文字：
{"breakfast":{"name":"早餐名称","calories":"热量大卡","foods":["食材1","食材2"]},"lunch":{"name":"午餐名称","calories":"热量大卡","foods":["食材1","食材2"]},"dinner":{"name":"晚餐名称","calories":"热量大卡","foods":["食材1","食材2"]},"tips":["建议1","建议2"]}
""";

        // 调用AI - 优先使用 agent（AiClient），失败再用 DashScope
        try {
            return agent.chat(prompt);
        } catch (Exception e) {
            log.warn("Agent调用失败，尝试备用方案: {}", e.getMessage());
            if (dashScopeConfig.isConfigured()) {
                try {
                    return callDashScopeAI(prompt);
                } catch (Exception ex) {
                    log.error("备用AI调用也失败: {}", ex.getMessage());
                }
            }
            return "AI服务暂时不可用，请稍后重试。";
        }
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
            log.error("解析AI响应失败: {}", e.getMessage());
            throw new RuntimeException("AI响应解析失败");
        }
    }

    private Map<String, Object> generateFallbackRecommendation(UserHealth health) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 根据BMI确定推荐策略
        String bmiLevel = "正常";
        if (health != null && health.getBmi() != null) {
            double bmi = health.getBmi().doubleValue();
            if (bmi >= 24) bmiLevel = "偏胖";
            else if (bmi < 18.5) bmiLevel = "偏瘦";
        }

        // 早餐
        Map<String, Object> breakfast = new LinkedHashMap<>();
        breakfast.put("name", "营养早餐");
        breakfast.put("calories", "500-600大卡");
        breakfast.put("foods", Arrays.asList("全麦面包", "牛奶", "鸡蛋", "水果"));
        breakfast.put("suggestion", "蛋白质丰富，适合" + bmiLevel + "人群");
        result.put("breakfast", breakfast);

        // 午餐
        Map<String, Object> lunch = new LinkedHashMap<>();
        lunch.put("name", "健康午餐");
        lunch.put("calories", "700-800大卡");
        lunch.put("foods", Arrays.asList("米饭", "鸡胸肉", "青菜", "番茄蛋汤"));
        lunch.put("suggestion", "荤素搭配，营养均衡");
        result.put("lunch", lunch);

        // 晚餐
        Map<String, Object> dinner = new LinkedHashMap<>();
        dinner.put("name", "轻盈晚餐");
        dinner.put("calories", "400-500大卡");
        dinner.put("foods", Arrays.asList("杂粮饭", "鱼肉", "凉拌蔬菜"));
        dinner.put("suggestion", "少油少盐，避免夜宵");
        result.put("dinner", dinner);

        // 建议
        List<String> tips = new ArrayList<>();
        tips.add("保持规律饮食时间");
        tips.add("细嚼慢咽，每餐20分钟以上");
        tips.add("如需增重可适当加餐");
        if ("偏胖".equals(bmiLevel)) {
            tips.add("建议减少油炸食品摄入");
            tips.add("优先选择清蒸、白灼烹饪方式");
        }
        result.put("tips", tips);

        return result;
    }

}