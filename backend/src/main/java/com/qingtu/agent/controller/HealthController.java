package com.qingtu.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final UserHealthMapper userHealthMapper;
    private final JwtUtil jwtUtil;

    @GetMapping("/kpi")
    public CommonResult<?> getKpi(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return CommonResult.fail("未登录");
        }

        UserHealth health = userHealthMapper.selectOne(
            new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0)
        );

        if (health == null) {
            return CommonResult.fail("暂无健康数据");
        }

        return CommonResult.success(calculateKpi(health));
    }

    @PutMapping("/update")
    public CommonResult<?> updateHealth(HttpServletRequest request, @RequestBody UserHealth healthData) {
        Long userId = getUserId(request);
        if (userId == null) {
            return CommonResult.fail("未登录");
        }

        UserHealth health = userHealthMapper.selectOne(
            new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0)
        );

        if (health == null) {
            health = new UserHealth();
            health.setUserId(userId);
        }

        if (healthData.getAge() != null) health.setAge(healthData.getAge());
        if (healthData.getGender() != null) health.setGender(healthData.getGender());
        if (healthData.getHeight() != null) {
            health.setHeight(healthData.getHeight());
            Double bmi = calculateBmi(health.getWeight() != null ? health.getWeight().doubleValue() : 0, 
                                      health.getHeight().doubleValue());
            if (bmi > 0) health.setBmi(BigDecimal.valueOf(bmi));
        }
        if (healthData.getWeight() != null) {
            health.setWeight(healthData.getWeight());
            Double bmi = calculateBmi(health.getWeight().doubleValue(), 
                                      health.getHeight() != null ? health.getHeight().doubleValue() : 0);
            if (bmi > 0) health.setBmi(BigDecimal.valueOf(bmi));
        }
        if (healthData.getActivityLevel() != null) {
            health.setActivityLevel(healthData.getActivityLevel());
        }

        if (health.getId() == null) {
            userHealthMapper.insert(health);
        } else {
            userHealthMapper.updateById(health);
        }

        return CommonResult.success(calculateKpi(health));
    }

    @GetMapping("/info")
    public CommonResult<?> getInfo(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return CommonResult.fail("未登录");
        }

        UserHealth health = userHealthMapper.selectOne(
            new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0)
        );

        if (health == null) {
            return CommonResult.success(new HashMap<>());
        }

        Map<String, Object> info = new HashMap<>();
        info.put("age", health.getAge());
        info.put("gender", health.getGender());
        info.put("height", health.getHeight());
        info.put("weight", health.getWeight());
        info.put("activityLevel", health.getActivityLevel());
        return CommonResult.success(info);
    }

    private Map<String, Object> calculateKpi(UserHealth health) {
        Map<String, Object> kpi = new HashMap<>();
        
        BigDecimal height = health.getHeight();
        BigDecimal weight = health.getWeight();
        Integer age = health.getAge();
        String gender = health.getGender();
        BigDecimal activityLevel = health.getActivityLevel();

        if (height != null && height.doubleValue() > 0 && weight != null && weight.doubleValue() > 0) {
            double bmi = calculateBmi(weight.doubleValue(), height.doubleValue());
            kpi.put("bmi", Math.round(bmi * 10) / 10.0);
            kpi.put("bmiLevel", getBmiLevel(bmi));
        }

        if (age != null && height != null && weight != null && gender != null) {
            double bmr = calculateBmr(weight.doubleValue(), height.doubleValue(), age, gender);
            kpi.put("bmr", (int) Math.round(bmr));
            kpi.put("dailyCalories", (int) Math.round(bmr * (activityLevel != null ? activityLevel.doubleValue() : 1.2)));
        }

        return kpi;
    }

    private double calculateBmi(double weight, double height) {
        if (height == 0) return 0;
        return weight / Math.pow(height / 100, 2);
    }

    private String getBmiLevel(double bmi) {
        if (bmi < 18.5) return "偏瘦";
        if (bmi < 24) return "正常";
        if (bmi < 28) return "偏胖";
        return "肥胖";
    }

    private double calculateBmr(double weight, double height, int age, String gender) {
        if ("M".equals(gender)) {
            return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
        } else {
            return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
        }
    }

    private Long getUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtUtil.getUserId(token);
            }
        } catch (Exception e) {
            log.error("解析token失败", e);
        }
        return null;
    }
}