package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.CalorieService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/calorie")
@RequiredArgsConstructor
public class CalorieController {

    private final CalorieService calorieService;
    private final JwtUtil jwtUtil;

    @PostMapping("/intake")
    public CommonResult<?> recordIntake(HttpServletRequest request,
                                  @RequestBody Map<String, Object> params) {
        Long userId = getUserIdFromRequest(request);
        String mealType = params.get("mealType").toString();
        String foodInput = params.get("foodInput").toString();
        return calorieService.recordIntake(userId, mealType, foodInput);
    }

    @GetMapping("/progress")
    public CommonResult<?> getTodayProgress(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return calorieService.getTodayProgress(userId);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}