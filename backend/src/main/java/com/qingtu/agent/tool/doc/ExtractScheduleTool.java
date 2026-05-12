package com.qingtu.agent.tool.doc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.entity.dto.CourseImportDTO;
import com.qingtu.agent.tool.ToolDefinition;
import com.qingtu.agent.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 课表抽取工具
 * 从课表文本中提取结构化课程信息，输出 ScheduleJSON 格式
 */
@Slf4j
@Component
public class ExtractScheduleTool implements ToolExecutor {

    private final ObjectProvider<QingTuAgent> qingTuAgentProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtractScheduleTool(ObjectProvider<QingTuAgent> qingTuAgentProvider) {
        this.qingTuAgentProvider = qingTuAgentProvider;
    }

    @Override
    public String getName() {
        return "extract_schedule";
    }

    @Override
    public String getDescription() {
        return "从课表文本（OCR结果或文档解析结果）中提取结构化课程信息，必须输出符合 ScheduleJSON 规范的 JSON";
    }

    @Override
    public String getCategory() {
        return "doc";
    }

    @Override
    @SuppressWarnings("removal")
    public ToolDefinition.ExecuteResult execute(Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        try {
            QingTuAgent qingTuAgent = qingTuAgentProvider.getObject();
            String rawText = (String) arguments.get("raw_text");
            Map<String, Object> semesterInfo = (Map<String, Object>) arguments.getOrDefault("semester_info", new HashMap<>());

            if (rawText == null || rawText.isBlank()) {
                return ToolDefinition.ExecuteResult.error("raw_text 参数不能为空");
            }

            String schoolName = (String) semesterInfo.getOrDefault("school_name", "");
            String semesterStart = (String) semesterInfo.getOrDefault("start_date", "2025-03-03");

            // 调用 LLM 抽取课表
            String prompt = buildScheduleExtractPrompt(rawText, schoolName, semesterStart);
            String llmResponse = qingTuAgent.chat(prompt);

            // 解析 LLM 响应为 ScheduleJSON
            ScheduleExtractionResult result = parseScheduleResponse(llmResponse);

            log.info("课表抽取完成: 课程数={}, 澄清问题数={}",
                    result.schedules.size(), result.clarifyingQuestions.size());

            return ToolDefinition.ExecuteResult.success(result);

        } catch (Exception e) {
            log.error("课表抽取失败", e);
            return ToolDefinition.ExecuteResult.error("课表抽取失败: " + e.getMessage());
        }
    }

    private String buildScheduleExtractPrompt(String rawText, String schoolName, String semesterStart) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 角色定义\n");
        prompt.append("你是一位专业的课表信息提取助手，负责从课表文本中提取结构化课程信息。\n\n");
        prompt.append("# 核心规则\n");
        prompt.append("1. **严格输出 JSON**：只输出 JSON 格式，不要任何解释或前缀\n");
        prompt.append("2. **强类型约束**：每个字段必须符合预定义的类型和范围\n");
        prompt.append("3. **置信度评估**：根据文本清晰度给每个字段打分（0-1）\n");
        prompt.append("4. **不确定字段标记**：遇到模糊信息必须生成 clarifying_questions\n\n");
        prompt.append("# 字段处理规则\n");
        prompt.append("- course_name: 课程名称，最大 100 字符\n");
        prompt.append("- weekday: 星期 1-7（1=周一，7=周日）\n");
        prompt.append("- time_slot: 时间格式 HH:mm-HH:mm\n");
        prompt.append("  - 标准节次：1-2节=08:00-09:40, 3-4节=10:00-11:40, 5-6节=14:00-15:40\n");
        prompt.append("  - 7-8节=16:00-17:40, 9-10节=19:00-20:40\n");
        prompt.append("- week_pattern: 周次模式 type=[weekly/odd/even], start_week, end_week\n");
        prompt.append("- confidence: 置信度 0-1\n");
        prompt.append("- uncertain_fields: 不确定的字段列表\n\n");
        prompt.append("# 澄清机制\n");
        prompt.append("当课程名/时间/地点/周次不明确时，生成 clarifying_questions：\n");
        prompt.append("```json\n");
        prompt.append("{\"field\": \"week_pattern\", \"question\": \"单周还是双周？\", \"options\": [\"单周\", \"双周\", \"每周\"]}\n");
        prompt.append("```\n\n");
        prompt.append("# 输入信息\n");
        if (!schoolName.isBlank()) {
            prompt.append("学校：").append(schoolName).append("\n");
        }
        prompt.append("学期开始日期：").append(semesterStart).append("\n");
        prompt.append("课表文本：\n").append(rawText).append("\n\n");
        prompt.append("# 输出要求\n");
        prompt.append("请输出以下格式的 JSON：\n");
        prompt.append("{\n");
        prompt.append("  \"schedules\": [...],\n");
        prompt.append("  \"clarifying_questions\": [...]\n");
        prompt.append("}\n");
        return prompt.toString();
    }

    private ScheduleExtractionResult parseScheduleResponse(String llmResponse) {
        ScheduleExtractionResult result = new ScheduleExtractionResult();
        result.schedules = new ArrayList<>();
        result.clarifyingQuestions = new ArrayList<>();

        try {
            // 提取 JSON 部分
            int startIdx = llmResponse.indexOf('{');
            int endIdx = llmResponse.lastIndexOf('}');
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonStr = llmResponse.substring(startIdx, endIdx + 1);
                Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);

                List<Map<String, Object>> schedulesRaw =
                        (List<Map<String, Object>>) parsed.getOrDefault("schedules", new ArrayList<>());
                for (Map<String, Object> s : schedulesRaw) {
                    ScheduleItem item = new ScheduleItem();
                    item.courseName = (String) s.getOrDefault("course_name", "");
                    item.weekday = ((Number) s.getOrDefault("weekday", 1)).intValue();
                    item.startTime = (String) s.getOrDefault("startTime", "");
                    item.endTime = (String) s.getOrDefault("endTime", "");
                    item.location = (String) s.getOrDefault("location", "");
                    item.teacher = (String) s.getOrDefault("teacher", "");
                    item.weekStart = ((Number) s.getOrDefault("weekStart", 1)).intValue();
                    item.weekEnd = ((Number) s.getOrDefault("weekEnd", 20)).intValue();
                    item.confidence = ((Number) s.getOrDefault("confidence", 0.8)).doubleValue();
                    result.schedules.add(item);
                }

                List<Map<String, Object>> questionsRaw =
                        (List<Map<String, Object>>) parsed.getOrDefault("clarifying_questions", new ArrayList<>());
                for (Map<String, Object> q : questionsRaw) {
                    ClarifyingQuestion question = new ClarifyingQuestion();
                    question.field = (String) q.getOrDefault("field", "");
                    question.question = (String) q.getOrDefault("question", "");
                    question.options = (List<String>) q.getOrDefault("options", new ArrayList<>());
                    result.clarifyingQuestions.add(question);
                }
            }
        } catch (Exception e) {
            log.error("解析课表响应失败", e);
        }

        return result;
    }

    // 内部数据结构
    public static class ScheduleExtractionResult {
        public List<ScheduleItem> schedules;
        public List<ClarifyingQuestion> clarifyingQuestions;
    }

    public static class ScheduleItem {
        public String courseName;
        public int weekday;
        public String startTime;
        public String endTime;
        public String location;
        public String teacher;
        public int weekStart = 1;
        public int weekEnd = 20;
        public double confidence = 0.8;
        public List<String> uncertainFields = new ArrayList<>();
    }

    public static class ClarifyingQuestion {
        public String field;
        public String question;
        public List<String> options = new ArrayList<>();
    }
}
