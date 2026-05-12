package com.qingtu.agent.agent.agent;

import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.entity.po.CostRecord;
import com.qingtu.agent.mapper.CostRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseSpecialistAgent {

    private final CostRecordMapper costRecordMapper;

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();

        try {
            return switch (action.toLowerCase()) {
                case "create" -> createExpense(taskId, context, params, correlationId);
                case "query" -> queryExpense(taskId, context, params, correlationId);
                default -> ResultMessage.failure(taskId, "expense", action, "未知动作: " + action, correlationId, context.getUserId());
            };
        } catch (Exception e) {
            log.error("记账执行失败", e);
            return ResultMessage.failure(taskId, "expense", action, e.getMessage(), correlationId, context.getUserId());
        }
    }

    private ResultMessage createExpense(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        Double amount = extractAmount(params);
        String category = extractCategory(params);
        String description = params.getOrDefault("description", params.getOrDefault("remark", "")).toString();

        if (amount == null || amount <= 0) {
            return ResultMessage.failure(taskId, "expense", "create", "金额必须大于0", correlationId, context.getUserId());
        }

        CostRecord record = new CostRecord();
        record.setUserId(context.getUserId());
        record.setAmount(BigDecimal.valueOf(amount));
        record.setCategory(category);
        record.setRemark(description);
        record.setSource("manual");
        record.setTradeTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        costRecordMapper.insert(record);

        log.info("记账成功: userId={}, costRecordId={}, amount={}, category={}",
                context.getUserId(), record.getId(), amount, category);

        return ResultMessage.success(taskId, "expense", "create",
                Map.of(
                        "costRecordId", record.getId(),
                        "amount", amount,
                        "category", category,
                        "message", "已为您记账：" + getCategoryName(category) + " " + amount + " 元"
                ),
                correlationId, context.getUserId());
    }

    private ResultMessage queryExpense(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        return ResultMessage.success(taskId, "expense", "query",
                Map.of("message", "查询记账功能开发中"),
                correlationId, context.getUserId());
    }

    private Double extractAmount(Map<String, Object> params) {
        String[] amountKeys = {"amount", "total", "money", "cost", "fee", "花费", "用了", "花了"};
        for (String key : amountKeys) {
            Object amountObj = params.get(key);
            if (amountObj != null) {
                if (amountObj instanceof Number) {
                    return ((Number) amountObj).doubleValue();
                }
                try {
                    String str = amountObj.toString().replaceAll("[^0-9.]", "");
                    if (!str.isBlank()) {
                        return Double.parseDouble(str);
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private String extractCategory(Map<String, Object> params) {
        Object categoryObj = params.getOrDefault("category", null);
        if (categoryObj != null && !categoryObj.toString().isBlank()) {
            return mapToEnglishCategory(categoryObj.toString());
        }

        Object typeObj = params.getOrDefault("type", null);
        if (typeObj != null && !typeObj.toString().isBlank()) {
            return mapToEnglishCategory(typeObj.toString());
        }

        Object descObj = params.getOrDefault("description", null);
        if (descObj != null) {
            return mapToEnglishCategory(descObj.toString());
        }

        return "other";
    }

    private String mapToEnglishCategory(String type) {
        return switch (type.toLowerCase()) {
            case "food", "饮食", "吃", "餐饮", "餐" -> "food";
            case "transport", "交通", "出行", "车", "公交", "地铁", "打车" -> "transport";
            case "shop", "shopping", "购物", "买", "商品" -> "shopping";
            case "entertainment", "娱乐", "玩", "游戏", "电影" -> "entertainment";
            case "life", "生活", "日常" -> "life";
            case "study", "学习", "书", "课程" -> "study";
            default -> "other";
        };
    }

    private String getCategoryName(String category) {
        return switch (category) {
            case "food" -> "饮食";
            case "transport" -> "交通";
            case "shopping" -> "购物";
            case "entertainment" -> "娱乐";
            case "life" -> "生活";
            case "study" -> "学习";
            default -> "其他";
        };
    }
}