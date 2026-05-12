package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

/**
 * 食堂菜品服务接口
 * 
 * 定义食堂菜品相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface DishService {

    /**
     * 分页查询菜品
     * 
     * @param type 菜品类型（breakfast/lunch/dinner/snack）
     * @param category 菜品种类
     * @param page 页码
     * @param size 每页数量
     * @return 菜品列表
     */
    CommonResult<?> listDishes(String type, String category, int page, int size);

    /**
     * 获取菜品详情
     * 
     * @param dishId 菜品ID
     * @return 菜品详情
     */
    CommonResult<?> getDishById(Long dishId);

    /**
     * AI推荐菜品
     * 
     * @param userId 用户ID
     * @param mealType 餐食类型（breakfast/lunch/dinner）
     * @return 推荐菜品
     */
    CommonResult<?> recommendDishes(Long userId, String mealType);

    /**
     * 获取今日推荐
     * 
     * @param userId 用户ID
     * @return 今日三餐推荐
     */
    CommonResult<?> getTodayRecommendation(Long userId);

    void refreshTodayRecommendationAsync(Long userId);
}