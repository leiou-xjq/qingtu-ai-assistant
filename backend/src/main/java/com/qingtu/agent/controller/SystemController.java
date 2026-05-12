package com.qingtu.agent.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统信息控制器
 * 
 * 提供系统信息、健康检查等接口
 * 
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "青途智伴AI生活助手");
        result.put("version", "1.0.0");
        result.put("description", "面向大学生全场景全自动AI生活助手");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}