package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.DishService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 食堂菜品控制器
 * 
 * 提供菜品浏览、AI推荐等功能
 * 
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final JwtUtil jwtUtil;

    /**
     * 分页查询菜品
     */
    @GetMapping("/list")
    public CommonResult<?> listDishes(@RequestParam(required = false) String type,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return dishService.listDishes(type, category, page, size);
    }

    /**
     * 获取菜品详情
     */
    @GetMapping("/{id}")
    public CommonResult<?> getDishById(@PathVariable Long id) {
        return dishService.getDishById(id);
    }

    /**
     * AI推荐菜品
     */
    @GetMapping("/recommend")
    public CommonResult<?> recommendDishes(HttpServletRequest request,
                                           @RequestParam String mealType) {
        Long userId = getUserIdFromRequest(request);
        return dishService.recommendDishes(userId, mealType);
    }

    /**
     * 获取今日推荐
     */
    @GetMapping("/today")
    public CommonResult<?> getTodayRecommendation(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return dishService.getTodayRecommendation(userId);
    }

    /**
     * 刷新今日食谱（绕过缓存重新生成）
     */
    @PostMapping("/today/refresh")
    public CommonResult<?> refreshTodayRecommendation(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        // 异步刷新缓存
        dishService.refreshTodayRecommendationAsync(userId);
        return CommonResult.success("正在刷新食谱...");
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}