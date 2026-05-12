package com.qingtu.agent.util;

/**
 * BMI计算工具类
 * 
 * 功能说明：
 * - 根据身高体重计算BMI值
 * - 判断BMI所属的健康区间
 * - 提供饮食目标建议
 * 
 * BMI计算公式：体重(kg) / 身高(m)²
 * 
 * BMI参考区间：
 * - < 18.5：偏瘦
 * - 18.5 ~ 24：正常
 * - 24 ~ 28：偏胖
 * - > 28：肥胖
 * 
 * @author 青途智伴技术团队
 */
public class BmiUtil {

    /**
     * 计算BMI值
     * 
     * @param heightCm 身高（厘米）
     * @param weightKg 体重（公斤）
     * @return BMI值（保留两位小数）
     */
    public static double calculateBmi(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) {
            throw new IllegalArgumentException("身高和体重必须大于0");
        }
        double heightM = heightCm / 100.0;
        return Math.round(weightKg / (heightM * heightM) * 100.0) / 100.0;
    }

    /**
     * 获取BMI健康评估
     * 
     * @param bmi BMI值
     * @return 健康评估结果
     */
    public static BmiResult evaluateBmi(double bmi) {
        if (bmi < 18.5) {
            return new BmiResult("偏瘦", "体重偏低，建议适当增加营养摄入", "gain");
        } else if (bmi < 24) {
            return new BmiResult("正常", "继续保持健康的生活方式", "balance");
        } else if (bmi < 28) {
            return new BmiResult("偏胖", "建议适当控制饮食，加强运动", "lose");
        } else {
            return new BmiResult("肥胖", "建议制定科学的减重计划", "lose");
        }
    }

    /**
     * 计算每日建议摄入卡路里
     * 
     * @param heightCm 身高（厘米）
     * @param weightKg 体重（公斤）
     * @param age 年龄
     * @param gender 性别（M/F）
     * @param activityLevel 活动水平（1.2久坐/1.375轻量/1.55中等/1.75活跃）
     * @return 每日建议摄入卡路里
     */
    public static int calculateDailyCalories(double heightCm, double weightKg, int age, char gender, double activityLevel) {
        double bmr;
        if (gender == 'M') {
            bmr = 88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age);
        } else {
            bmr = 447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age);
        }
        return (int) Math.round(bmr * activityLevel);
    }

    /**
     * 根据饮食目标调整卡路里
     * 
     * @param baseCalories 基础代谢消耗
     * @param goal 目标（lose减脂/gain增肌/balance均衡）
     * @return 调整后的每日建议摄入卡路里
     */
    public static int adjustCaloriesForGoal(int baseCalories, String goal) {
        return switch (goal) {
            case "lose" -> baseCalories - 300;
            case "gain" -> baseCalories + 300;
            default -> baseCalories;
        };
    }

    /**
     * BMI结果封装类
     */
    public record BmiResult(String level, String suggestion, String recommendedGoal) {}
}