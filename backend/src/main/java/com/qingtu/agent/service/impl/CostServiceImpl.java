package com.qingtu.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.service.CostService;
import com.qingtu.agent.util.BaiduOcrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingtu.agent.entity.po.CostRecord;
import com.qingtu.agent.mapper.CostRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostServiceImpl implements CostService {

    private final CostRecordMapper costMapper;
    private final BaiduOcrUtil baiduOcrUtil;
    private final DashScopeConfig dashScopeConfig;
    private final QingTuAgent qingTuAgent;

    @Override
    public CommonResult<?> listCostRecords(Long userId, String category, String startDate, String endDate,
                                       Integer year, Integer month, int page, int size) {
        LambdaQueryWrapper<CostRecord> wrapper = new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getUserId, userId)
                .eq(CostRecord::getDeleted, 0)
                .orderByDesc(CostRecord::getTradeTime);

        // 如果提供了年月，优先使用年月筛选
        if (year != null && month != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            wrapper.ge(CostRecord::getTradeTime, start.atStartOfDay());
            wrapper.le(CostRecord::getTradeTime, end.atTime(23, 59, 59));
        } else {
            // 否则使用日期范围
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(CostRecord::getTradeTime, LocalDate.parse(startDate).atStartOfDay());
            }
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(CostRecord::getTradeTime, LocalDate.parse(endDate).atTime(23, 59, 59));
            }
        }

        if (category != null && !category.isEmpty()) {
            wrapper.eq(CostRecord::getCategory, category);
        }

        Page<CostRecord> pageResult = new Page<>(page, size);
        costMapper.selectPage(pageResult, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> addCostRecord(Long userId, BigDecimal amount, String category, String remark,
                                       String merchantName, String tradeTime) {
        CostRecord record = new CostRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setCategory(category);
        record.setRemark(remark);
        record.setMerchantName(merchantName);
        record.setSource("manual");

        if (tradeTime != null && !tradeTime.isEmpty()) {
            try {
                record.setTradeTime(LocalDate.parse(tradeTime).atStartOfDay());
            } catch (DateTimeParseException e) {
                record.setTradeTime(LocalDateTime.now());
            }
        } else {
            record.setTradeTime(LocalDateTime.now());
        }

        costMapper.insert(record);
        return CommonResult.success("添加成功");
    }

    @Override
    public CommonResult<?> deleteCostRecord(Long userId, Long recordId) {
        CostRecord record = costMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            return CommonResult.fail(ResultCode.COST_RECORD_NOT_FOUND);
        }
        record.setDeleted(1);
        costMapper.updateById(record);
        return CommonResult.success("删除成功");
    }

    @Override
    public CommonResult<?> importBill(Long userId, MultipartFile file, String source) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lines = content.split("\n");
            int imported = 0;

            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                CostRecord record = new CostRecord();
                record.setUserId(userId);
                record.setSource(source != null ? source : "wechat");

                try {
                    record.setAmount(new BigDecimal(parts[0].trim()));
                    record.setCategory(classifyByMerchant(parts.length > 1 ? parts[1].trim() : ""));
                    record.setMerchantName(parts.length > 1 ? parts[1].trim() : "");
                    record.setTradeTime(LocalDateTime.now());
                    costMapper.insert(record);
                    imported++;
                } catch (Exception e) {
                    // 跳过格式错误的行
                }
            }

            return CommonResult.success(imported);
        } catch (Exception e) {
            return CommonResult.fail("导入失败：" + e.getMessage());
        }
    }

    @Override
    public CommonResult<?> ocrBill(Long userId, MultipartFile file) {
        try {
            // 将图片转为Base64
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

            // TODO: 调用实际的OCR服务（如百度OCR、腾讯OCR等）
            // 这里先用模拟数据返回，实际需要接入OCR服务
            String ocrText = callOcrService(base64Image);

            // 解析OCR文本中的账单信息
            List<Map<String, Object>> billItems = parseBillFromOcrText(ocrText);

            return CommonResult.success(billItems);
        } catch (Exception e) {
            return CommonResult.fail("识别失败：" + e.getMessage());
        }
    }

    /**
     * 调用OCR服务（使用百度OCR）
     */
    private String callOcrService(String base64Image) {
        try {
            String result = baiduOcrUtil.recognizeText(base64Image);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.error("百度OCR识别失败", e);
        }
        // 如果OCR失败，返回模拟数据用于演示
        return "微信支付\n" +
               "¥25.50\n" +
               "2024-01-15 12:30\n" +
               "沙县小吃\n\n" +
               "微信支付\n" +
               "¥4.00\n" +
               "2024-01-15 18:20\n" +
               "地铁出行";
    }

    /**
     * 从OCR文本中解析账单信息
     * 微信/支付宝账单截图识别后的文本通常是：
     * [时间] [商户名] [金额]
     */
    private List<Map<String, Object>> parseBillFromOcrText(String ocrText) {
        List<Map<String, Object>> items = new ArrayList<>();

        if (ocrText == null || ocrText.isEmpty()) {
            return items;
        }

        // 匹配金额：¥XX.XX 或 XX.XX 格式
        Pattern amountPattern = Pattern.compile("¥?(\\d+\\.\\d{1,2})");

        String[] lines = ocrText.split("\n|\r");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher amountMatcher = amountPattern.matcher(line);
            if (amountMatcher.find()) {
                try {
                    String amountStr = amountMatcher.group(1);
                    BigDecimal amount = new BigDecimal(amountStr);

                    // 金额范围过滤：0.01 - 5000
                    if (amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("5000")) <= 0) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("amount", amount.setScale(2, RoundingMode.HALF_UP));

                        // 提取时间
                        String dateStr = extractDate(line);
                        if (dateStr == null) {
                            dateStr = LocalDate.now().toString();
                        }
                        item.put("time", dateStr);

                        // 提取商户名
                        String merchant = extractMerchant(line);
                        item.put("merchant", merchant);

                        // 智能分类
                        item.put("category", classifyByMerchant(merchant));

                        items.add(item);
                    }
                } catch (NumberFormatException ignored) {
                    // 跳过无效金额
                }
            }
        }

        return items;
    }

    private String extractDate(String line) {
        // 匹配日期格式：2024-01-15 或 2024/01/15 或 01-15
        Pattern datePattern = Pattern.compile("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?)");
        Matcher matcher = datePattern.matcher(line);
        if (matcher.find()) {
            return normalizeDate(matcher.group(1));
        }

        // 短日期格式：01-15
        Pattern shortPattern = Pattern.compile("(\\d{1,2}[-/]\\d{1,2}[日]?)");
        Matcher shortMatcher = shortPattern.matcher(line);
        if (shortMatcher.find()) {
            String date = shortMatcher.group(1);
            return LocalDate.now().getYear() + "-" + normalizeDate(date);
        }

        return null;
    }

    private String extractMerchant(String line) {
        // 常见商户关键词
        String[][] keywords = {
            {"超市", "购物", "商场", "便利店", "华润", "大润发", "永辉", "沃尔玛", "家乐福"},
            {"餐饮", "餐厅", "饭店", "酒楼", "火锅", "烧烤", "小龙坎", "海底捞"},
            {"快餐", "麦当劳", "肯德基", "汉堡王", "沙县", "兰州拉面", "黄焖鸡"},
            {"奶茶", "咖啡", "茶颜", "喜茶", "奈雪", "星巴克", "瑞幸"},
            {"外卖", "美团", "饿了么"},
            {"地铁", "公交", "出行", "打车", "滴滴", "出租车", "停车", "加油", "高速"},
            {"电影", "影院", "万达", "CGV"},
            {"网购", "淘宝", "京东", "天猫", "拼多多", "唯品会"},
            {"话费", "移动", "联通", "电信"},
            {"水电", "燃气", "暖气"}
        };

        String[] categories = {"shopping", "food", "food", "food", "food",
                "transport", "entertainment", "shopping", "life", "life"};

        for (int i = 0; i < keywords.length; i++) {
            for (String keyword : keywords[i]) {
                if (line.contains(keyword)) {
                    return keyword;
                }
            }
        }

        // 如果没有匹配关键词，返回行首部分作为商户
        if (line.length() > 0 && line.length() <= 20 && !line.matches(".*\\d{2}:\\d{2}.*")) {
            return line;
        }

        return "其他消费";
    }

    private String normalizeDate(String dateStr) {
        try {
            dateStr = dateStr.replace("年", "-").replace("月", "-").replace("日", "");
            String[] parts = dateStr.split("[-/\\s]");
            if (parts.length >= 3) {
                return parts[0] + "-" +
                       String.format("%02d", Integer.parseInt(parts[1])) + "-" +
                       String.format("%02d", Integer.parseInt(parts[2].split(":")[0]));
            }
        } catch (Exception ignored) {}
        return LocalDate.now().toString();
    }

    private boolean containsMerchantKeyword(String text) {
        String[] keywords = {"餐饮", "美食", "快餐", "饭店", "超市", "商场", "淘宝", "京东", "外卖",
                           "地铁", "公交", "打车", "电影", "游戏", "娱乐", "学习", "图书", "课程"};
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String extractMerchantName(String text) {
        // 简单提取商户名
        String[] parts = text.split("[-/\\s]");
        for (String part : parts) {
            if (part.length() >= 2 && part.length() <= 20) {
                return part;
            }
        }
        return text.length() > 20 ? text.substring(0, 20) : text;
    }

    private String classifyByMerchant(String merchantName) {
        if (merchantName == null || merchantName.isEmpty()) return "other";

        merchantName = merchantName.toLowerCase();

        if (containsAny(merchantName, "餐饮", "美食", "快餐", "饭店", "外卖", "餐厅", "小吃", "火锅", "烧烤", "奶茶", "咖啡")) {
            return "food";
        }
        if (containsAny(merchantName, "地铁", "公交", "出行", "打车", "滴滴", "出租", "停车", "加油")) {
            return "transport";
        }
        if (containsAny(merchantName, "电影", "游戏", "娱乐", "KTV", "酒吧", "旅游", "景区")) {
            return "entertainment";
        }
        if (containsAny(merchantName, "淘宝", "京东", "超市", "购物", "商城", "天猫", "拼多多")) {
            return "shopping";
        }
        if (containsAny(merchantName, "水电", "房租", "物业", "生活", "话费", "网费")) {
            return "life";
        }
        if (containsAny(merchantName, "学习", "图书", "课程", "培训", "教育", "文具")) {
            return "study";
        }
        return "other";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    @Override
    public CommonResult<?> getMonthlyStatistics(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        var records = costMapper.selectList(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getUserId, userId)
                .ge(CostRecord::getTradeTime, start.atStartOfDay())
                .le(CostRecord::getTradeTime, end.atTime(23, 59, 59))
                .eq(CostRecord::getDeleted, 0));

        BigDecimal total = records.stream().map(CostRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var categoryStats = records.stream()
                .collect(Collectors.groupingBy(CostRecord::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, CostRecord::getAmount, BigDecimal::add)));

        int daysInMonth = end.getDayOfMonth();
        BigDecimal dailyAverage = total.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", total);
        result.put("recordCount", records.size());
        result.put("dailyAverage", dailyAverage);
        result.put("categoryStats", categoryStats);

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> getCategoryStatistics(Long userId, String startDate, String endDate) {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        return getMonthlyStatistics(userId, year, month);
    }

    @Override
    public CommonResult<?> getMonthlyReport(Long userId, int year, int month) {
        var stats = getMonthlyStatistics(userId, year, month);
        Map<String, Object> data = (Map<String, Object>) stats.getData();

        // 获取上月数据进行对比
        int prevYear = month == 1 ? year - 1 : year;
        int prevMonth = month == 1 ? 12 : month - 1;
        var prevStats = getMonthlyStatistics(userId, prevYear, prevMonth);
        Map<String, Object> prevData = (Map<String, Object>) prevStats.getData();

        // 构建分析数据
        String currentOverview = String.format("%d年%d月消费报告", year, month);
        int recordCount = (int) data.get("recordCount");
        BigDecimal totalAmount = (BigDecimal) data.get("totalAmount");
        BigDecimal dailyAverage = (BigDecimal) data.get("dailyAverage");

        int prevRecordCount = (int) prevData.get("recordCount");
        BigDecimal prevTotalAmount = (BigDecimal) prevData.get("totalAmount");

        @SuppressWarnings("unchecked")
        Map<String, Object> categoryStats = (Map<String, Object>) data.get("categoryStats");
        @SuppressWarnings("unchecked")
        Map<String, Object> prevCategoryStats = (Map<String, Object>) prevData.get("categoryStats");

        // 如果没有消费记录，返回提示
        if (recordCount == 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("overview", "本月暂无消费记录，继续保持！");
            result.put("structure", new ArrayList<>());
            result.put("abnormal", "");
            result.put("suggestions", new ArrayList<>(Arrays.asList("记录每一笔消费，养成良好的消费习惯")));
            return CommonResult.success(result);
        }

        // 构建分类对比字符串
        StringBuilder categoryCompare = new StringBuilder();
        categoryCompare.append(String.format("本月总支出 ¥%s，共%d笔消费，日均 ¥%s\n",
                totalAmount, recordCount, dailyAverage));
        categoryCompare.append("分类明细：\n");

        List<Map.Entry<String, Object>> sortedCategories = categoryStats.entrySet().stream()
                .sorted((a, b) -> new BigDecimal(b.getValue().toString())
                        .compareTo(new BigDecimal(a.getValue().toString())))
                .collect(Collectors.toList());

        for (Map.Entry<String, Object> entry : sortedCategories) {
            String category = entry.getKey();
            BigDecimal amount = new BigDecimal(entry.getValue().toString());
            BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            String prevAmountStr = prevCategoryStats != null ? prevCategoryStats.getOrDefault(category, "0").toString() : "0";
            BigDecimal prevAmount = new BigDecimal(prevAmountStr);

            String trend = "";
            if (prevAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = amount.subtract(prevAmount).divide(prevAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    trend = String.format(" (↑%.1f%%)", change);
                } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                    trend = String.format(" (↓%.1f%%)", change.abs());
                }
            }

            categoryCompare.append(String.format("- %s: ¥%s (占比%.1f%%)%s\n",
                    getCategoryName(category), amount, percentage, trend));
        }

        // 调用AI生成分析
        try {
            String aiAnalysis = callAIForAnalysis(year, month, recordCount, totalAmount,
                    prevRecordCount, prevTotalAmount, categoryStats, prevCategoryStats, categoryCompare.toString());

            return CommonResult.success(parseAIAnalysis(aiAnalysis));
        } catch (Exception e) {
            log.error("AI分析失败，使用备用方案", e);
            return CommonResult.success(generateFallbackReport(year, month, totalAmount, dailyAverage,
                    recordCount, categoryStats, prevCategoryStats));
        }
    }

    private String callAIForAnalysis(int year, int month, int recordCount, BigDecimal totalAmount,
                                     int prevRecordCount, BigDecimal prevTotalAmount,
                                     Map<String, Object> categoryStats, Map<String, Object> prevCategoryStats,
                                     String categoryCompare) {
        String prompt = String.format("""
                请分析以下月度消费数据，生成结构化的消费分析报告。

                【本月数据】
                - 时间：%d年%d月
                - 消费笔数：%d笔
                - 总支出：¥%s
                - 上月消费笔数：%d笔
                - 上月总支出：¥%s

                【分类详情】
                %s

                请生成包含以下四个部分的JSON格式分析报告（确保输出是有效的JSON）：
                {
                    "overview": "消费概览（1-2句话总结本月整体消费情况）",
                    "structure": ["分类1的详细分析", "分类2的详细分析", ...],
                    "abnormal": "异常消费提醒（如果有明显异常支出或不合理消费）",
                    "suggestions": ["建议1", "建议2", "建议3"]
                }

                分析要点：
                1. 与上月对比消费变化趋势
                2. 识别占比最高的消费分类
                3. 指出可能存在问题的消费（如某分类占比过高、某分类突增等）
                4. 根据消费习惯给出切实可行的优化建议
                """, year, month, recordCount, totalAmount, prevRecordCount,
                prevTotalAmount, categoryCompare);

        // 优先使用 QingTuAgent（AiClient），失败再用 DashScope
        try {
            return qingTuAgent.chat(prompt, null);
        } catch (Exception e) {
            log.warn("QingTuAgent调用失败，尝试备用方案: {}", e.getMessage());
            if (dashScopeConfig.isConfigured()) {
                try {
                    return callDashScopeAI(prompt);
                } catch (Exception ex) {
                    log.error("备用AI调用也失败: {}", ex.getMessage());
                }
            }
            return generateFallbackAnalysis(year, month, totalAmount, categoryStats);
        }
    }

    private String callDashScopeAI(String prompt) {
        try {
            String url = dashScopeConfig.getBaseUrl() + "/compatible-mode/v1/services/aigc/text-generation/generation";

            Map<String, Object> input = new HashMap<>();
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            input.put("messages", messages);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", dashScopeConfig.getModel());
            requestBody.put("input", input);
            Map<String, Object> params = new HashMap<>();
            params.put("result_format", "message");
            requestBody.put("parameters", params);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode output = root.path("output");
            JsonNode choices = output.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            return root.path("output").path("text").asText();
        } catch (Exception e) {
            log.error("调用DashScope AI失败: {}", e.getMessage());
            throw new RuntimeException("AI服务调用失败", e);
        }
    }

    private Map<String, Object> parseAIAnalysis(String aiResponse) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 尝试解析JSON
            int jsonStart = aiResponse.indexOf("{");
            int jsonEnd = aiResponse.lastIndexOf("}");
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonStr = aiResponse.substring(jsonStart, jsonEnd + 1);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonStr);

                result.put("overview", node.path("overview").asText("本月消费情况总体平稳"));
                result.put("structure", parseArrayField(node.path("structure")));
                result.put("abnormal", node.path("abnormal").asText(""));
                result.put("suggestions", parseArrayField(node.path("suggestions")));
            } else {
                return generateFallbackFromText(aiResponse);
            }
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
            return generateFallbackFromText(aiResponse);
        }
        return result;
    }

    private List<String> parseArrayField(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    private Map<String, Object> generateFallbackFromText(String text) {
        Map<String, Object> result = new HashMap<>();
        result.put("overview", "本月消费情况详见以下分析");
        result.put("structure", new ArrayList<>(Arrays.asList(text)));
        result.put("abnormal", "");
        result.put("suggestions", new ArrayList<>(Arrays.asList("建议保持良好消费习惯")));
        return result;
    }

    private Map<String, Object> generateFallbackReport(int year, int month, BigDecimal totalAmount,
                                                        BigDecimal dailyAverage, int recordCount,
                                                        Map<String, Object> categoryStats,
                                                        Map<String, Object> prevCategoryStats) {
        Map<String, Object> result = new HashMap<>();

        result.put("overview", String.format("%d年%d月共消费%d笔，总支出¥%s，日均¥%s",
                year, month, recordCount, totalAmount, dailyAverage));

        List<String> structure = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Object amount : categoryStats.values()) {
            total = total.add(new BigDecimal(amount.toString()));
        }
        for (Map.Entry<String, Object> entry : categoryStats.entrySet()) {
            BigDecimal amount = new BigDecimal(entry.getValue().toString());
            BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            structure.add(String.format("%s占比%.1f%%，消费¥%s",
                    getCategoryName(entry.getKey()), percentage, amount));
        }
        result.put("structure", structure);

        // 简单异常检测
        List<String> abnormalList = new ArrayList<>();
        BigDecimal maxCategory = BigDecimal.ZERO;
        String maxCategoryName = "";
        for (Map.Entry<String, Object> entry : categoryStats.entrySet()) {
            BigDecimal amount = new BigDecimal(entry.getValue().toString());
            if (amount.compareTo(maxCategory) > 0) {
                maxCategory = amount;
                maxCategoryName = entry.getKey();
            }
        }
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxPercentage = maxCategory.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (maxPercentage.compareTo(BigDecimal.valueOf(50)) > 0) {
                abnormalList.add(String.format("%s占比%.1f%%，超过总消费50%%，建议关注",
                        getCategoryName(maxCategoryName), maxPercentage));
            }
        }
        result.put("abnormal", abnormalList.isEmpty() ? "未发现明显异常消费" : String.join("；", abnormalList));

        List<String> suggestions = new ArrayList<>();
        suggestions.add("建议养成每日记账的习惯");
        suggestions.add("大额消费前先思考是否必要");
        if ("food".equals(getMaxCategory(categoryStats))) {
            suggestions.add("餐饮支出较大，可考虑自己做饭");
        }
        result.put("suggestions", suggestions);

        return result;
    }

    private String generateFallbackAnalysis(int year, int month, BigDecimal totalAmount,
                                           Map<String, Object> categoryStats) {
        int recordCount = categoryStats.values().stream().mapToInt(v -> 1).sum();
        return String.format("{\"overview\":\"%d年%d月消费总结\",\"structure\":[\"共%d笔消费\",\"总支出¥%s\"],\"abnormal\":\"\",\"suggestions\":[\"建议保持良好消费习惯\"]}",
                year, month, recordCount, totalAmount);
    }

    private String getMaxCategory(Map<String, Object> categoryStats) {
        String maxCategory = "";
        BigDecimal maxAmount = BigDecimal.ZERO;
        for (Map.Entry<String, Object> entry : categoryStats.entrySet()) {
            BigDecimal amount = new BigDecimal(entry.getValue().toString());
            if (amount.compareTo(maxAmount) > 0) {
                maxAmount = amount;
                maxCategory = entry.getKey();
            }
        }
        return maxCategory;
    }

    private String getCategoryName(String category) {
        Map<String, String> categoryNames = Map.of(
                "food", "饮食",
                "transport", "交通",
                "entertainment", "娱乐",
                "shopping", "购物",
                "life", "生活",
                "study", "学习",
                "other", "其他"
        );
        return categoryNames.getOrDefault(category, category);
    }
}