package com.qingtu.agent.entity.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 健康档案VO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class HealthVO implements Serializable {

    private Long id;

    private Double height;

    private Double weight;

    private Double bmi;

    private String bmiLevel;

    private String bmiSuggestion;

    private Integer age;

    private String gender;

    private Double activityLevel;

    private String activityLevelName;

    private String dietGoal;

    private String dietGoalName;

    private Integer dailyCalories;

    private String tabooFood;

    private String tastePreference;

    private String healthSuggestion;
}