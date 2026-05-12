package com.qingtu.agent.agent.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.UserHealthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileSpecialistAgent {

    private final UserHealthMapper userHealthMapper;

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "height", "身高",
            "weight", "体重",
            "tastePreference", "口味偏好",
            "age", "年龄",
            "gender", "性别",
            "dietGoal", "饮食目标",
            "activityLevel", "活动水平",
            "tabooFood", "饮食忌口"
    );

    private static final Map<String, String> PROTECTED_FIELDS = Map.of(
            "id", "ID",
            "userId", "用户ID"
    );

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String taskId = java.util.UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();

        try {
            return switch (action.toLowerCase()) {
                case "update" -> updateProfile(taskId, context, params, correlationId);
                case "query" -> queryProfile(taskId, context, params, correlationId);
                default -> ResultMessage.failure(taskId, "profile", action, "未知动作: " + action, correlationId, context.getUserId());
            };
        } catch (Exception e) {
            log.error("资料修改失败", e);
            return ResultMessage.failure(taskId, "profile", action, e.getMessage(), correlationId, context.getUserId());
        }
    }

    private ResultMessage updateProfile(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        UserHealth health = getOrCreateHealth(context.getUserId());
        if (health == null) {
            return ResultMessage.failure(taskId, "profile", "update", "无法创建健康档案", correlationId, context.getUserId());
        }

        Map<String, String> updatedFields = new HashMap<>();
        StringBuilder messages = new StringBuilder();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (field.startsWith("_")) continue;
            if (value == null || value.toString().isBlank()) continue;

            if (PROTECTED_FIELDS.containsKey(field)) {
                messages.append(PROTECTED_FIELDS.get(field)).append("无法修改。");
                continue;
            }

            switch (field) {
                case "height" -> {
                    try {
                        double h = ((Number) value).doubleValue();
                        if (h < 50 || h > 300) {
                            messages.append("身高必须在50-300之间。");
                        } else {
                            health.setHeight(BigDecimal.valueOf(h));
                            updatedFields.put("height", h + "cm");
                            recalculateBmi(health);
                        }
                    } catch (Exception e) {
                        messages.append("身高格式不正确。");
                    }
                }
                case "weight" -> {
                    try {
                        double w = ((Number) value).doubleValue();
                        if (w < 20 || w > 500) {
                            messages.append("体重必须在20-500之间。");
                        } else {
                            health.setWeight(BigDecimal.valueOf(w));
                            updatedFields.put("weight", w + "kg");
                            recalculateBmi(health);
                        }
                    } catch (Exception e) {
                        messages.append("体重格式不正确。");
                    }
                }
                case "tastePreference" -> {
                    health.setTastePreference(value.toString());
                    updatedFields.put("tastePreference", value.toString());
                }
                case "age" -> {
                    try {
                        int age = ((Number) value).intValue();
                        if (age < 1 || age > 150) {
                            messages.append("年龄必须在1-150之间。");
                        } else {
                            health.setAge(age);
                            updatedFields.put("age", String.valueOf(age));
                        }
                    } catch (Exception e) {
                        messages.append("年龄格式不正确。");
                    }
                }
                case "gender" -> {
                    String gender = value.toString().toUpperCase();
                    if ("M".equals(gender) || "F".equals(gender) || "男".equals(gender) || "女".equals(gender)) {
                        health.setGender("M".equals(gender) || "男".equals(gender) ? "M" : "F");
                        updatedFields.put("gender", "M".equals(health.getGender()) ? "男" : "女");
                    } else {
                        messages.append("性别只能选择男或女。");
                    }
                }
                case "dietGoal" -> {
                    String dietGoal = value.toString().toLowerCase();
                    if ("lose".equals(dietGoal) || "gain".equals(dietGoal) || "balance".equals(dietGoal) ||
                            "减脂".equals(dietGoal) || "增肌".equals(dietGoal) || "均衡".equals(dietGoal)) {
                        health.setDietGoal(switch (dietGoal) {
                            case "lose", "减脂" -> "lose";
                            case "gain", "增肌" -> "gain";
                            default -> "balance";
                        });
                        updatedFields.put("dietGoal", dietGoal);
                    } else {
                        messages.append("饮食目标只能是减脂、增肌或均衡。");
                    }
                }
                case "activityLevel" -> {
                    try {
                        double level = ((Number) value).doubleValue();
                        if (level < 1.0 || level > 2.0) {
                            messages.append("活动水平必须在1.0-2.0之间。");
                        } else {
                            health.setActivityLevel(BigDecimal.valueOf(level));
                            updatedFields.put("activityLevel", String.valueOf(level));
                        }
                    } catch (Exception e) {
                        messages.append("活动水平格式不正确。");
                    }
                }
                case "tabooFood" -> {
                    health.setTabooFood(value.toString());
                    updatedFields.put("tabooFood", value.toString());
                }
                default -> {
                    if (!FIELD_LABELS.containsKey(field)) {
                        log.debug("忽略未知字段: {}", field);
                    }
                }
            }
        }

        if (!updatedFields.isEmpty()) {
            health.setUpdateTime(LocalDateTime.now());
            userHealthMapper.updateById(health);
            log.info("资料修改成功: userId={}, fields={}", context.getUserId(), updatedFields.keySet());
        }

        StringBuilder resultMsg = new StringBuilder();
        if (!updatedFields.isEmpty()) {
            resultMsg.append("已更新：");
            for (Map.Entry<String, String> entry : updatedFields.entrySet()) {
                String label = FIELD_LABELS.getOrDefault(entry.getKey(), entry.getKey());
                resultMsg.append(label).append("=").append(entry.getValue()).append("，");
            }
            resultMsg.deleteCharAt(resultMsg.length() - 1);
        }

        if (health.getBmi() != null) {
            String bmiSuggestion = getBmiSuggestion(health.getBmi());
            if (resultMsg.length() > 0) resultMsg.append("。");
            resultMsg.append(bmiSuggestion);
        }

        if (messages.length() > 0) {
            if (resultMsg.length() > 0) resultMsg.append("。");
            resultMsg.append(messages);
        }

        if (resultMsg.length() == 0) {
            resultMsg.append("未找到需要修改的字段");
        }

        return ResultMessage.success(taskId, "profile", "update",
                Map.of(
                        "updatedFields", updatedFields.keySet().stream().map(k -> FIELD_LABELS.getOrDefault(k, k)).toList(),
                        "bmi", health.getBmi(),
                        "message", resultMsg.toString()
                ),
                correlationId, context.getUserId());
    }

    private ResultMessage queryProfile(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        UserHealth health = getUserHealth(context.getUserId());

        Map<String, Object> profile = new HashMap<>();
        if (health != null) {
            profile.put("height", health.getHeight());
            profile.put("weight", health.getWeight());
            profile.put("bmi", health.getBmi());
            profile.put("age", health.getAge());
            profile.put("gender", health.getGender());
            profile.put("tastePreference", health.getTastePreference());
            profile.put("dietGoal", health.getDietGoal());
            profile.put("activityLevel", health.getActivityLevel());
            profile.put("dailyCalories", health.getDailyCalories());
            profile.put("tabooFood", health.getTabooFood());
        } else {
            profile.put("message", "未找到健康档案，请先完善资料");
        }

        return ResultMessage.success(taskId, "profile", "query", profile, correlationId, context.getUserId());
    }

    private UserHealth getOrCreateHealth(Long userId) {
        UserHealth health = userHealthMapper.selectOne(
                new LambdaQueryWrapper<UserHealth>()
                        .eq(UserHealth::getUserId, userId)
                        .eq(UserHealth::getDeleted, 0)
        );

        if (health == null) {
            health = new UserHealth();
            health.setUserId(userId);
            health.setCreateTime(LocalDateTime.now());
            health.setActivityLevel(BigDecimal.valueOf(1.2));
            health.setDietGoal("balance");
            userHealthMapper.insert(health);
            log.info("创建健康档案: userId={}", userId);
        }

        return health;
    }

    private UserHealth getUserHealth(Long userId) {
        return userHealthMapper.selectOne(
                new LambdaQueryWrapper<UserHealth>()
                        .eq(UserHealth::getUserId, userId)
                        .eq(UserHealth::getDeleted, 0)
        );
    }

    private void recalculateBmi(UserHealth health) {
        if (health.getHeight() != null && health.getWeight() != null &&
                health.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            double heightM = health.getHeight().doubleValue() / 100;
            double bmi = health.getWeight().doubleValue() / (heightM * heightM);
            health.setBmi(BigDecimal.valueOf(bmi).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private String getBmiSuggestion(BigDecimal bmi) {
        if (bmi == null) return "";
        double bmiVal = bmi.doubleValue();
        if (bmiVal < 18.5) {
            return "您的BMI为" + bmiVal + "（偏瘦），建议适当增加营养摄入";
        } else if (bmiVal < 24) {
            return "您的BMI为" + bmiVal + "（正常），继续保持健康生活方式";
        } else if (bmiVal < 28) {
            return "您的BMI为" + bmiVal + "（偏胖），建议适当控制饮食";
        } else {
            return "您的BMI为" + bmiVal + "（肥胖），建议制定减重计划";
        }
    }
}