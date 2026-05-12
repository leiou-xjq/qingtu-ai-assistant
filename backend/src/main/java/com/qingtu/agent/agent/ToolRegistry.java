package com.qingtu.agent.agent;

import com.qingtu.agent.common.Constants;
import lombok.Data;

/**
 * 工具注册表
 * 
 * 功能说明：
 * - 定义AI Agent可调用的工具
 * - 封装工具调用参数和描述
 * - 支持工具执行结果返回
 * 
 * @author 青途智伴技术团队
 */
@Data
public class ToolRegistry {

    /**
     * 可用工具列表
     */
    public static final String[] AVAILABLE_TOOLS = {
        "getWeather", "getOutfitSuggestion", "getDietRecommendation",
        "getTodayCourses", "addCostRecord", "generateCourseNote",
        "ragSearch", "sendNotification"
    };

    /**
     * 获取工具描述
     */
    public static String getToolDescription(String toolName) {
        return switch (toolName) {
            case "getWeather" -> "获取指定城市的天气信息，包括温度、湿度、风力等";
            case "getOutfitSuggestion" -> "根据天气情况获取穿衣建议";
            case "getDietRecommendation" -> "获取健康饮食推荐";
            case "getTodayCourses" -> "获取今日课程安排";
            case "addCostRecord" -> "添加消费记录";
            case "generateCourseNote" -> "生成课程AI笔记";
            case "ragSearch" -> "搜索校园知识库相关内容";
            case "sendNotification" -> "发送站内消息通知";
            default -> "未知工具";
        };
    }

    /**
     * 获取工具参数schema
     */
    public static String getToolParamsSchema(String toolName) {
        return switch (toolName) {
            case "getWeather" -> "{\"location\": \"城市名称\"}";
            case "getDietRecommendation" -> "{\"mealType\": \"breakfast/lunch/dinner\"}";
            case "addCostRecord" -> "{\"amount\": 10.5, \"category\": \"food\", \"remark\": \"备注\"}";
            case "ragSearch" -> "{\"query\": \"搜索关键词\", \"topK\": 5}";
            case "sendNotification" -> "{\"title\": \"标题\", \"content\": \"内容\"}";
            default -> "{}";
        };
    }
}