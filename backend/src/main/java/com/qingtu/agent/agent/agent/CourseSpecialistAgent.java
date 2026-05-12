package com.qingtu.agent.agent.agent;

import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.service.ParseJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.*;

/**
 * 课程专家 Agent
 * 处理课程导入、查询等操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseSpecialistAgent {

    private final CourseScheduleMapper courseScheduleMapper;
    private final ParseJobService parseJobService;

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params, Map<String, String> files) {
        long startTime = System.currentTimeMillis();
        String taskId = java.util.UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();

        try {
            return switch (action.toLowerCase()) {
                case "import" -> importCourse(taskId, context, params, files, correlationId);
                case "query" -> queryCourse(taskId, context, params, correlationId);
                default -> ResultMessage.failure(taskId, "course", action, "未知动作: " + action, correlationId, context.getUserId());
            };
        } catch (Exception e) {
            log.error("课程操作失败", e);
            return ResultMessage.failure(taskId, "course", action, e.getMessage(), correlationId, context.getUserId());
        }
    }

    private ResultMessage importCourse(String taskId, UserContext context, Map<String, Object> params,
                                       Map<String, String> files, String correlationId) {
        if (files == null || files.isEmpty()) {
            return ResultMessage.failure(taskId, "course", "import", "请上传课程文件", correlationId, context.getUserId());
        }

        String fileName = null;
        String fileData = null;
        String fileType = "image";

        for (Map.Entry<String, String> entry : files.entrySet()) {
            fileName = entry.getKey();
            fileData = entry.getValue();
            fileType = getFileType(fileName);
            break;
        }

        List<CourseSchedule> courses = new ArrayList<>();

        try {
            if (isBase64Image(fileData)) {
                courses = parseImageFile(fileData, fileName, context);
            } else if (isTextContent(fileData)) {
                courses = parseTextContent(fileData, context);
            }
        } catch (Exception e) {
            log.error("解析课程文件失败", e);
            return ResultMessage.failure(taskId, "course", "import", "解析课程文件失败: " + e.getMessage(), correlationId, context.getUserId());
        }

        if (courses.isEmpty()) {
            return ResultMessage.failure(taskId, "course", "import", "未能识别出课程信息，请检查文件内容", correlationId, context.getUserId());
        }

        for (CourseSchedule course : courses) {
            course.setUserId(context.getUserId());
            course.setCreateTime(java.time.LocalDateTime.now());
            course.setUpdateTime(java.time.LocalDateTime.now());
            courseScheduleMapper.insert(course);
        }

        log.info("课程导入成功: userId={}, courses={}", context.getUserId(), courses.size());

        return ResultMessage.success(taskId, "course", "import",
                Map.of(
                        "coursesImported", courses.size(),
                        "courses", courses.stream().map(c -> Map.of(
                                "name", c.getName() != null ? c.getName() : "",
                                "weekday", c.getWeekday(),
                                "time", c.getStartTime() + "-" + c.getEndTime(),
                                "location", c.getLocation() != null ? c.getLocation() : ""
                        )).toList(),
                        "message", "已导入 " + courses.size() + " 门课程到您的课表"
                ),
                correlationId, context.getUserId());
    }

    private List<CourseSchedule> parseImageFile(String base64Data, String fileName, UserContext context) {
        return new ArrayList<>();
    }

    private List<CourseSchedule> parseTextContent(String content, UserContext context) {
        List<CourseSchedule> courses = new ArrayList<>();

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            CourseSchedule course = new CourseSchedule();
            course.setUserId(context.getUserId());

            if (line.contains("周") && line.contains("节")) {
                String[] parts = line.split("[\\s,，]+");
                for (String part : parts) {
                    if (part.contains("周")) {
                        course.setWeekday(extractWeekday(part));
                    }
                    if (part.contains("节")) {
                        int[] times = extractTimeSlot(part);
                        if (times != null) {
                            course.setStartTime(LocalTime.of(times[0], 0));
                            course.setEndTime(LocalTime.of(times[1], 0));
                        }
                    }
                }

                int colonIdx = line.indexOf('：');
                if (colonIdx > 0) {
                    course.setName(line.substring(0, colonIdx));
                } else {
                    course.setName(line.substring(0, Math.min(50, line.length())));
                }

                course.setLocation("");
                course.setTeacher("");
                course.setWeekStart(1);
                course.setWeekEnd(20);

                courses.add(course);
            }
        }

        return courses;
    }

    private int extractWeekday(String text) {
        String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < weekdays.length; i++) {
            if (text.contains(weekdays[i])) {
                return i + 1;
            }
        }
        return 1;
    }

    private int[] extractTimeSlot(String text) {
        if (text.contains("1-2")) return new int[]{8, 10};
        if (text.contains("3-4")) return new int[]{10, 12};
        if (text.contains("5-6")) return new int[]{14, 16};
        if (text.contains("7-8")) return new int[]{16, 18};
        if (text.contains("9-10")) return new int[]{19, 21};
        return null;
    }

    private ResultMessage queryCourse(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        return ResultMessage.success(taskId, "course", "query",
                Map.of("message", "查询课程功能开发中"),
                correlationId, context.getUserId());
    }

    private boolean isBase64Image(String data) {
        return data != null && (data.startsWith("/9j/") || data.startsWith("iVBOR") || data.startsWith("data:image"));
    }

    private boolean isTextContent(String data) {
        return data != null && !isBase64Image(data) && data.length() < 10000;
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "image";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv")) return "excel";
        if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt")) return "document";
        return "image";
    }
}