package com.qingtu.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.CourseKeyPoint;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.CourseKeyPointMapper;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.service.CourseProgressService;
import com.qingtu.agent.task.WeekUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 课程进度服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseProgressServiceImpl implements CourseProgressService {

    private final CourseScheduleMapper courseMapper;
    private final CourseKeyPointMapper noteMapper;
    private final UserMapper userMapper;
    private final QingTuAgent agent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CommonResult<?> analyzeCourseProgress(Long userId, Long courseId) {
        CourseSchedule course = courseMapper.selectById(courseId);
        if (course == null) {
            return CommonResult.fail("课程不存在");
        }
        if (userId != null && !userId.equals(course.getUserId())) {
            return CommonResult.fail("无权限访问该课程");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.fail("用户不存在");
        }

        LocalDate semesterStart = user.getSemesterStart();
        if (semesterStart == null) {
            semesterStart = LocalDate.of(2025, 3, 3);
        }
        WeekUtil.setSemesterStart(semesterStart);
        int currentWeek = WeekUtil.getCurrentWeek(null);
        WeekUtil.clearSemesterStart();

        int courseWeekStart = course.getWeekStart() != null ? course.getWeekStart() : 1;
        int courseWeekEnd = course.getWeekEnd() != null ? course.getWeekEnd() : 20;
        int totalCourseWeeks = courseWeekEnd - courseWeekStart + 1;

        int weeksPassed = currentWeek - courseWeekStart + 1;
        if (weeksPassed < 1) weeksPassed = 1;
        if (weeksPassed > totalCourseWeeks) weeksPassed = totalCourseWeeks;

        double progressPercent = (double) weeksPassed / totalCourseWeeks * 100;

        String stage = "基础阶段";
        if (progressPercent >= 75) {
            stage = "冲刺阶段";
        } else if (progressPercent >= 50) {
            stage = "进阶阶段";
        }

        String analysisPrompt = String.format(
            "请分析课程「%s」的当前教学进度：\n" +
            "- 当前学期周数：第%d周\n" +
            "- 本课程教学周数：第%d至第%d周（共%d周）\n" +
            "- 课程进度：约%.0f%%（%s）\n\n" +
            "请给出：\n" +
            "1. 教学进度评估\n" +
            "2. 接下来的重点内容预测\n" +
            "3. 学习建议",
            course.getName(), currentWeek, courseWeekStart, courseWeekEnd, totalCourseWeeks,
            progressPercent, stage
        );

        String analysis = agent.chat(analysisPrompt, null);

        Map<String, Object> result = new HashMap<>();
        result.put("courseName", course.getName());
        result.put("courseWeekStart", courseWeekStart);
        result.put("courseWeekEnd", courseWeekEnd);
        result.put("totalCourseWeeks", totalCourseWeeks);
        result.put("currentWeek", currentWeek);
        result.put("progressPercent", progressPercent);
        result.put("stage", stage);
        result.put("analysis", analysis);

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> generateCourseNote(Long userId, Long courseId, Integer weekNum) {
        CourseSchedule course = courseMapper.selectById(courseId);
        if (course == null) {
            return CommonResult.fail("课程不存在");
        }
        if (userId != null && !userId.equals(course.getUserId())) {
            return CommonResult.fail("无权限访问该课程");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return CommonResult.fail("用户不存在");
        }

        LocalDate semesterStart = user.getSemesterStart();
        if (semesterStart == null) {
            semesterStart = LocalDate.of(2025, 3, 3);
        }
        WeekUtil.setSemesterStart(semesterStart);
        int currentWeek = weekNum != null ? weekNum : WeekUtil.getCurrentWeek(null);
        WeekUtil.clearSemesterStart();

        String courseInfo = String.format("课程名称：%s\n上课地点：%s\n上课时间：%s-%s\n教学周数：第%d至第%d周\n当前分析周数：第%d周",
                course.getName(),
                course.getLocation() != null ? course.getLocation() : "未指定",
                course.getStartTime() != null ? course.getStartTime().toString() : "08:00",
                course.getEndTime() != null ? course.getEndTime().toString() : "09:40",
                course.getWeekStart() != null ? course.getWeekStart() : 1,
                course.getWeekEnd() != null ? course.getWeekEnd() : 20,
                currentWeek);

        String notePrompt = "你是一个课程笔记生成助手。请根据以下课程信息生成详细的课程笔记。\n\n" +
                courseInfo + "\n\n" +
                "请严格按照以下JSON格式返回笔记内容：\n" +
                "{\n" +
                "  \"corePoints\": [\"核心知识点1\", \"核心知识点2\", \"核心知识点3\"],\n" +
                "  \"examPoints\": [\"考试重点1\", \"考试重点2\"],\n" +
                "  \"difficultPoints\": [\"难点1\", \"难点2\"],\n" +
                "  \"易错点\": [\"易错点1\", \"易错点2\"],\n" +
                "  \"reviewGuide\": \"复习指南内容\",\n" +
                "  \"summary\": \"课程总结内容\"\n" +
                "}";

        String noteJson = agent.chat(notePrompt, null);
        log.info("AI返回笔记JSON: {}", noteJson);

        CourseKeyPoint note = parseNoteJson(noteJson, userId, courseId, course.getName(), currentWeek, course);

        try {
            noteMapper.insert(note);
            return CommonResult.success(note);
        } catch (Exception e) {
            log.error("保存笔记失败", e);
            return CommonResult.fail("保存笔记失败：" + e.getMessage());
        }
    }

    private CourseKeyPoint parseNoteJson(String noteJson, Long userId, Long courseId, String courseName, int currentWeek, CourseSchedule course) {
        CourseKeyPoint note = new CourseKeyPoint();
        note.setUserId(userId);
        note.setCourseId(courseId);
        note.setCourseName(courseName);
        note.setWeekNum(currentWeek);
        note.setClassDate(LocalDate.now());
        note.setClassTime((course.getStartTime() != null ? course.getStartTime().toString() : "08:00") + "-" +
                         (course.getEndTime() != null ? course.getEndTime().toString() : "09:40"));
        note.setAiModel("Qwen");

        try {
            JsonNode root = objectMapper.readTree(noteJson);

            JsonNode corePoints = root.get("corePoints");
            note.setCorePoints(corePoints != null ? objectMapper.writeValueAsString(corePoints) : "[\"待补充\"]");

            JsonNode examPoints = root.get("examPoints");
            note.setExamPoints(examPoints != null ? objectMapper.writeValueAsString(examPoints) : "[\"待补充\"]");

            JsonNode difficultPoints = root.get("difficultPoints");
            note.setDifficultPoints(difficultPoints != null ? objectMapper.writeValueAsString(difficultPoints) : "[\"待补充\"]");

            JsonNode yiCuoDian = root.get("易错点");
            note.set易错点(yiCuoDian != null ? objectMapper.writeValueAsString(yiCuoDian) : "[\"待补充\"]");

            note.setReviewGuide(root.has("reviewGuide") ? root.get("reviewGuide").asText() : "请根据课程内容复习");
            note.setSummary(root.has("summary") ? root.get("summary").asText() : "课程笔记已生成");

        } catch (Exception e) {
            log.warn("JSON解析失败，使用默认值: {}", e.getMessage());
            note.setCorePoints("[\"解析失败\"]");
            note.setExamPoints("[\"解析失败\"]");
            note.setDifficultPoints("[\"解析失败\"]");
            note.set易错点("[\"解析失败\"]");
            note.setReviewGuide("JSON解析失败，请手动查看AI返回内容");
            note.setSummary("课程笔记已生成，详情请查看AI返回内容");
        }

        return note;
    }
}
