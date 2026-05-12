package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

public interface CalorieService {

    CommonResult<?> recordIntake(Long userId, String mealType, String foodInput);

    CommonResult<?> getTodayProgress(Long userId);
}