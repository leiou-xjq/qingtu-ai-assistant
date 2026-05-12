package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.WeatherService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 天气穿搭控制器
 * 
 * 提供天气查询、AI穿搭建议、早安推送等功能
 * 
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    private final JwtUtil jwtUtil;

    /**
     * 获取实时天气
     */
    @GetMapping("/current")
    public CommonResult<?> getCurrentWeather(HttpServletRequest request, @RequestParam(required = false) String location) {
        Long userId = getUserIdFromRequest(request);
        return weatherService.getCurrentWeather(userId, location);
    }

    /**
     * 获取天气预报
     */
    @GetMapping("/forecast")
    public CommonResult<?> getForecast(HttpServletRequest request,
                                        @RequestParam(required = false) String location,
                                        @RequestParam(defaultValue = "7") int days) {
        Long userId = getUserIdFromRequest(request);
        return weatherService.getForecast(userId, location, days);
    }

    /**
     * 获取AI穿搭建议
     */
    @GetMapping("/outfit")
    public CommonResult<?> getOutfitSuggestion(HttpServletRequest request,
            @RequestParam(required = false) String location) {
        Long userId = getUserIdFromRequest(request);
        return weatherService.getOutfitSuggestion(userId, location);
    }

    /**
     * 预加载穿搭建议（异步，后台执行）
     */
    @PostMapping("/outfit/preload")
    public CommonResult<?> preloadOutfitSuggestion(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        weatherService.preloadOutfitSuggestion(userId);
        return CommonResult.success("预加载已触发");
    }

    /**
     * 发送早安推送
     */
    @PostMapping("/morning-push")
    public CommonResult<?> sendMorningPush(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return weatherService.sendMorningPush(userId);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}