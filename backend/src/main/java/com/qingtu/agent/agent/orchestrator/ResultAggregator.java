package com.qingtu.agent.agent.orchestrator;

import com.qingtu.agent.agent.message.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.qingtu.agent.util.WeatherUtil.DailyForecast;

/**
 * 结果聚合器
 * 收集各 Agent 返回的结果，生成友好回复
 */
@Slf4j
@Component
public class ResultAggregator {

    public String aggregate(List<ResultMessage> results, String originalMessage) {
        if (results == null || results.isEmpty()) {
            return "处理完成，但没有返回结果。";
        }

        StringBuilder response = new StringBuilder();

        for (ResultMessage result : results) {
            String partial = formatResult(result);
            if (partial != null && !partial.isBlank()) {
                if (response.length() > 0) {
                    response.append("\n\n");
                }
                response.append(partial);
            }
        }

        return response.toString();
    }

    private String formatResult(ResultMessage result) {
        if (result == null) {
            return null;
        }

        if (!result.isSuccess() && !result.isFallback()) {
            return formatError(result);
        }

        return switch (result.getAgent().toLowerCase()) {
            case "weather" -> formatWeatherResult(result);
            case "expense" -> formatExpenseResult(result);
            case "course" -> formatCourseResult(result);
            case "profile" -> formatProfileResult(result);
            case "note" -> formatNoteResult(result);
            case "chat" -> formatChatResult(result);
            case "search" -> formatSearchResult(result);
            default -> formatDefaultResult(result);
        };
    }

    private String formatWeatherResult(ResultMessage result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getResult();
        if (data == null) return null;

        Object forecastsObj = data.get("forecasts");
        if (forecastsObj instanceof List) {
            return formatForecastResult(data);
        }

        String message = (String) data.getOrDefault("message", "");
        if (!message.isBlank()) {
            return message;
        }

        String city = (String) data.getOrDefault("city", "未知");
        String temp = String.valueOf(data.getOrDefault("temp", "--"));
        String text = (String) data.getOrDefault("text", "");
        String summary = (String) data.getOrDefault("summary", "");

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(city).append("天气】\n");
        sb.append("温度: ").append(temp).append("°C ").append(text).append("\n");
        if (!summary.isBlank()) {
            sb.append(summary);
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatForecastResult(Map<String, Object> data) {
        String city = (String) data.getOrDefault("city", "未知");
        List<DailyForecast> allForecasts = (List<DailyForecast>) data.get("forecasts");
        int dayOffset = 0;
        Object offsetObj = data.get("dayOffset");
        if (offsetObj != null) {
            try {
                dayOffset = Integer.parseInt(offsetObj.toString());
            } catch (NumberFormatException ignored) {}
        }

        List<DailyForecast> forecasts = allForecasts;
        if (dayOffset > 0 && forecasts.size() > dayOffset) {
            forecasts = forecasts.subList(dayOffset, forecasts.size());
        }

        StringBuilder sb = new StringBuilder();
        String prefix = dayOffset > 0 ? (dayOffset == 1 ? "明天" : (dayOffset == 2 ? "后天" : dayOffset + "天后")) : "";
        if (!prefix.isEmpty()) {
            sb.append("【").append(city).append(prefix).append("天气】\n");
        } else {
            sb.append("【").append(city).append("未来").append(forecasts.size()).append("天天气】\n");
        }

        for (DailyForecast f : forecasts) {
            sb.append(f.getDateDisplay());
            sb.append("：").append(f.getWeatherEmoji());
            sb.append(f.getTextDay());
            sb.append(" ").append(f.getTempLow()).append("-").append(f.getTempHigh()).append("°C");
            sb.append(" 紫外线：").append(f.getUvIndex());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatExpenseResult(ResultMessage result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getResult();
        if (data == null) return null;

        if (result.isFallback()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mockExpense = (Map<String, Object>) data.get("mockExpense");
            if (mockExpense != null) {
                return "⚠️ 记账服务暂时不可用，模拟记录：消费 " + mockExpense.get("amount") + " 元（" + mockExpense.get("category") + "）" + mockExpense.get("note");
            }
            return "⚠️ " + data.getOrDefault("message", "记账失败");
        }

        String message = (String) data.getOrDefault("message", "");
        if (!message.isBlank()) {
            return message;
        }

        Object expenseId = data.get("expenseId");
        Object amount = data.get("amount");
        Object category = data.get("category");

        if (expenseId != null) {
            return "✅ 已为您记账：" + category + " " + amount + " 元";
        }
        return "✅ 记账完成";
    }

    private String formatCourseResult(ResultMessage result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getResult();
        if (data == null) return null;

        if (result.isFallback()) {
            return "⚠️ " + data.getOrDefault("message", "课程导入失败");
        }

        String message = (String) data.getOrDefault("message", "");
        if (!message.isBlank()) {
            return message;
        }

        Object imported = data.get("coursesImported");
        if (imported != null) {
            return "✅ 已导入 " + imported + " 门课程到您的课表";
        }
        return "✅ 课程处理完成";
    }

    private String formatProfileResult(ResultMessage result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getResult();
        if (data == null) return null;

        if (result.isFallback()) {
            return "⚠️ " + data.getOrDefault("message", "资料修改失败");
        }

        String message = (String) data.getOrDefault("message", "");
        if (!message.isBlank()) {
            return message;
        }

        Object updated = data.get("updatedFields");
        if (updated != null) {
            return "✅ 已更新个人资料：" + updated;
        }
        return "✅ 资料修改完成";
    }

    private String formatNoteResult(ResultMessage result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getResult();
        if (data == null) return null;

        if (result.isFallback()) {
            return "⚠️ " + data.getOrDefault("message", "笔记生成失败");
        }

        Object noteId = data.get("noteId");
        Object title = data.get("title");
        Object content = data.get("content");

        if (noteId != null && title != null) {
            return "✅ 已为您生成笔记：「" + title + "」\n" + content;
        }

        String contentStr = content != null ? content.toString() : "";
        if (!contentStr.isBlank()) {
            return "📝 笔记内容：\n" + contentStr;
        }

        return "✅ 笔记生成完成";
    }

    private String formatChatResult(ResultMessage result) {
        Object data = result.getResult();
        if (data == null) return null;
        return data.toString();
    }

    private String formatSearchResult(ResultMessage result) {
        Object data = result.getResult();
        if (data == null) return null;
        return data.toString();
    }

    private String formatError(ResultMessage result) {
        String error = result.getErrorMessage();
        if (error == null || error.isBlank()) {
            return "⚠️ " + result.getAgent() + " 执行失败";
        }
        return "⚠️ " + result.getAgent() + " 失败：" + error;
    }

    private String formatDefaultResult(ResultMessage result) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getResult();
        if (data != null) {
            String message = (String) data.getOrDefault("message", "");
            if (!message.isBlank()) {
                return message;
            }
        }
        return "✅ " + result.getAgent() + " 处理完成";
    }
}