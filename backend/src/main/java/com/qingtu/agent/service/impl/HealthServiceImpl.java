package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.entity.dto.HealthRecordDTO;
import com.qingtu.agent.entity.po.UserHealth;
import com.qingtu.agent.entity.vo.HealthVO;
import com.qingtu.agent.exception.BusinessException;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.service.HealthService;
import com.qingtu.agent.util.BmiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康档案服务实现类
 * 
 * @author 青途智伴技术团队
 */
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final UserHealthMapper healthMapper;

    @Override
    public CommonResult<?> getHealthRecord(Long userId) {
        UserHealth health = healthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0));

        if (health == null) {
            return CommonResult.success("未找到健康档案", null);
        }

        return CommonResult.success(toHealthVO(health));
    }

    @Override
    public CommonResult<?> saveHealthRecord(Long userId, HealthRecordDTO dto) {
        UserHealth health = healthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0));

        boolean isNew = (health == null);
        if (isNew) {
            health = new UserHealth();
            health.setUserId(userId);
        }

        double bmi = BmiUtil.calculateBmi(dto.getHeight(), dto.getWeight());
        BmiUtil.BmiResult bmiResult = BmiUtil.evaluateBmi(bmi);

        health.setHeight(BigDecimal.valueOf(dto.getHeight()));
        health.setWeight(BigDecimal.valueOf(dto.getWeight()));
        health.setBmi(BigDecimal.valueOf(bmi).setScale(2, RoundingMode.HALF_UP));
        health.setAge(dto.getAge());
        health.setGender(dto.getGender());
        health.setActivityLevel(BigDecimal.valueOf(dto.getActivityLevel() != null ? dto.getActivityLevel() : 1.2));
        health.setDietGoal(dto.getDietGoal() != null ? dto.getDietGoal() : bmiResult.recommendedGoal());

        int dailyCalories = BmiUtil.calculateDailyCalories(
                dto.getHeight(), dto.getWeight(),
                dto.getAge() != null ? dto.getAge() : 20,
                dto.getGender().charAt(0),
                dto.getActivityLevel() != null ? dto.getActivityLevel() : 1.2
        );
        health.setDailyCalories(BmiUtil.adjustCaloriesForGoal(dailyCalories, health.getDietGoal()));

        if (dto.getTabooFood() != null) health.setTabooFood(dto.getTabooFood());
        if (dto.getTastePreference() != null) health.setTastePreference(dto.getTastePreference());
        health.setHealthSuggestion(bmiResult.suggestion());

        if (isNew) {
            healthMapper.insert(health);
        } else {
            healthMapper.updateById(health);
        }

        return CommonResult.success("保存成功", toHealthVO(health));
    }

    @Override
    public CommonResult<?> calculateBmi(Double height, Double weight) {
        double bmi = BmiUtil.calculateBmi(height, weight);
        BmiUtil.BmiResult result = BmiUtil.evaluateBmi(bmi);

        Map<String, Object> data = new HashMap<>();
        data.put("bmi", bmi);
        data.put("level", result.level());
        data.put("suggestion", result.suggestion());
        data.put("recommendedGoal", result.recommendedGoal());

        return CommonResult.success(data);
    }

    @Override
    public CommonResult<?> getHealthSuggestion(Long userId) {
        UserHealth health = healthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId)
                .eq(UserHealth::getDeleted, 0));

        if (health == null) {
            return CommonResult.fail(ResultCode.HEALTH_RECORD_NOT_FOUND);
        }

        StringBuilder suggestion = new StringBuilder();
        suggestion.append("当前BMI: ").append(health.getBmi()).append("\n");
        suggestion.append("健康评估: ").append(BmiUtil.evaluateBmi(health.getBmi().doubleValue()).level()).append("\n");
        suggestion.append("每日建议摄入: ").append(health.getDailyCalories()).append(" 千卡\n");

        if (health.getHealthSuggestion() != null) {
            suggestion.append("\n健康建议: ").append(health.getHealthSuggestion());
        }

        return CommonResult.success(suggestion.toString());
    }

    private HealthVO toHealthVO(UserHealth health) {
        HealthVO vo = new HealthVO();
        vo.setId(health.getId());
        vo.setHeight(health.getHeight().doubleValue());
        vo.setWeight(health.getWeight().doubleValue());
        vo.setBmi(health.getBmi().doubleValue());
        vo.setAge(health.getAge());
        vo.setGender(health.getGender());
        vo.setActivityLevel(health.getActivityLevel().doubleValue());
        vo.setDietGoal(health.getDietGoal());
        vo.setDailyCalories(health.getDailyCalories());
        vo.setTabooFood(health.getTabooFood());
        vo.setTastePreference(health.getTastePreference());
        vo.setHealthSuggestion(health.getHealthSuggestion());

        BmiUtil.BmiResult bmiResult = BmiUtil.evaluateBmi(health.getBmi().doubleValue());
        vo.setBmiLevel(bmiResult.level());
        vo.setBmiSuggestion(bmiResult.suggestion());
        vo.setDietGoalName(switch (health.getDietGoal()) {
            case "lose" -> "减脂";
            case "gain" -> "增肌";
            default -> "均衡";
        });
        String activityLevelName;
        double activity = health.getActivityLevel().doubleValue();
        if (activity == 1.375) {
            activityLevelName = "轻度活跃";
        } else if (activity == 1.55) {
            activityLevelName = "中等活跃";
        } else if (activity == 1.75) {
            activityLevelName = "高度活跃";
        } else {
            activityLevelName = "久坐";
        }
        vo.setActivityLevelName(activityLevelName);

        return vo;
    }
}