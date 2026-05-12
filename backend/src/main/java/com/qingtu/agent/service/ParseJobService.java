package com.qingtu.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.ParseJob;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.ParseJobMapper;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.tool.ToolRegistry;
import com.qingtu.agent.tool.doc.DocumentParseTool;
import com.qingtu.agent.tool.doc.ExtractScheduleTool;
import com.qingtu.agent.util.AliyunOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.util.*;

/**
 * 文档解析任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseJobService {

    private final ParseJobMapper parseJobMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final UserMapper userMapper;
    private final ToolRegistry toolRegistry;
    private final QingTuAgent qingTuAgent;
    private final AliyunOssUtil aliyunOSSUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRIES = 3;

    /**
     * 创建解析任务
     */
    public ParseJob createJob(Long userId, MultipartFile file, String fileType) {
        try {
            // 1. 上传文件到 OSS
            String fileUrl = aliyunOSSUtil.uploadFile(file);

            // 2. 创建任务
            ParseJob job = new ParseJob();
            job.setUserId(userId);
            job.setFileName(file.getOriginalFilename());
            job.setFileUrl(fileUrl);
            job.setFileType(fileType);
            job.setStatus("PENDING");
            job.setProgress(0);
            parseJobMapper.insert(job);

            log.info("创建解析任务: jobId={}, userId={}, file={}", job.getId(), userId, file.getOriginalFilename());

            // 3. 触发异步解析
            processJobAsync(job.getId());

            return job;

        } catch (Exception e) {
            log.error("创建解析任务失败", e);
            throw new RuntimeException("创建解析任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态
     */
    public ParseJob getJob(Long jobId) {
        return parseJobMapper.selectById(jobId);
    }

    /**
     * 异步处理解析任务
     */
    @Async("taskExecutor")
    public void processJobAsync(Long jobId) {
        ParseJob job = parseJobMapper.selectById(jobId);
        if (job == null) {
            log.error("任务不存在: jobId={}", jobId);
            return;
        }

        try {
            // 更新状态：PROCESSING
            updateProgress(jobId, 10, "PENDING");

            // Step 1: 解析文档
            Object parseResult = executeWithRetry("parse_document", Map.of(
                    "file_url", job.getFileUrl(),
                    "file_type", job.getFileType()
            ));
            if (parseResult == null) {
                throw new RuntimeException("文档解析失败");
            }
            updateProgress(jobId, 40, "PROCESSING");

            // Step 2: 如果是图片，进行 OCR
            String rawText;
            if ("image".equalsIgnoreCase(job.getFileType()) || isImageFile(job.getFileName())) {
                Object ocrResult = executeWithRetry("ocr_image", Map.of("image_url", job.getFileUrl()));
                rawText = ocrResult != null ? extractTextFromResult(ocrResult) : "";
            } else {
                rawText = extractTextFromResult(parseResult);
            }
            updateProgress(jobId, 60, "PROCESSING");

            // Step 3: 提取课表结构化数据
            User user = userMapper.selectById(job.getUserId());
            Map<String, Object> semesterInfo = new HashMap<>();
            if (user != null) {
                semesterInfo.put("school_name", user.getSchool() != null ? user.getSchool() : "");
                semesterInfo.put("start_date", user.getSemesterStart() != null
                        ? user.getSemesterStart().toString() : "2025-03-03");
            }

            Object scheduleResult = executeWithRetry("extract_schedule", Map.of(
                    "raw_text", rawText,
                    "semester_info", semesterInfo
            ));
            updateProgress(jobId, 85, "PROCESSING");

            // Step 4: 保存结果
            job.setResult(objectMapper.writeValueAsString(scheduleResult));
            job.setStatus("COMPLETED");
            job.setProgress(100);
            job.setCompletedAt(java.time.LocalDateTime.now());
            parseJobMapper.updateById(job);

            log.info("解析任务完成: jobId={}", jobId);

        } catch (Exception e) {
            log.error("解析任务失败: jobId={}", jobId, e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            parseJobMapper.updateById(job);
        }
    }

    /**
     * 人工确认解析结果
     */
    public void confirmJob(Long jobId, List<Map<String, Object>> confirmedSchedules) {
        ParseJob job = parseJobMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("任务不存在");
        }

        try {
            // 1. 保存确认数据
            job.setConfirmedData(objectMapper.writeValueAsString(confirmedSchedules));
            job.setStatus("CONFIRMED");
            job.setUpdatedAt(java.time.LocalDateTime.now());
            parseJobMapper.updateById(job);

            // 2. 插入课程数据
            for (Map<String, Object> schedule : confirmedSchedules) {
                insertCourse(job.getUserId(), schedule);
            }

            log.info("确认解析结果: jobId={}, courses={}", jobId, confirmedSchedules.size());

        } catch (Exception e) {
            log.error("确认解析结果失败", e);
            throw new RuntimeException("确认失败: " + e.getMessage());
        }
    }

    private void insertCourse(Long userId, Map<String, Object> schedule) {
        CourseSchedule course = new CourseSchedule();
        course.setUserId(userId);
        course.setName((String) schedule.getOrDefault("courseName", ""));

        Integer weekday = schedule.get("weekday") != null
                ? ((Number) schedule.get("weekday")).intValue() : 1;
        course.setWeekday(weekday);

        String location = (String) schedule.getOrDefault("location", "");
        course.setLocation(location);

        String teacher = (String) schedule.getOrDefault("teacher", "");
        course.setTeacher(teacher);

        Integer weekStart = schedule.get("weekStart") != null
                ? ((Number) schedule.get("weekStart")).intValue() : 1;
        course.setWeekStart(weekStart);

        Integer weekEnd = schedule.get("weekEnd") != null
                ? ((Number) schedule.get("weekEnd")).intValue() : 20;
        course.setWeekEnd(weekEnd);

        String startTime = (String) schedule.getOrDefault("startTime", "08:00");
        String endTime = (String) schedule.getOrDefault("endTime", "09:40");
        try {
            course.setStartTime(LocalTime.parse(startTime));
            course.setEndTime(LocalTime.parse(endTime));
        } catch (Exception ignored) {
            course.setStartTime(LocalTime.of(8, 0));
            course.setEndTime(LocalTime.of(9, 40));
        }

        course.setReminderEnabled(1);
        course.setReminderMinutes(15);

        courseScheduleMapper.insert(course);
    }

    private Object executeWithRetry(String toolName, Map<String, Object> arguments) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                var result = toolRegistry.executeTool(toolName, arguments);
                if (result.isSuccess()) {
                    return result.getData();
                }
                log.warn("工具执行失败，重试 {}/{}: {}", i + 1, MAX_RETRIES, result.getErrorMessage());
                Thread.sleep(1000L * (i + 1));  // 指数退避
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResult(Object result) {
        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;
            Object text = map.get("text");
            return text != null ? text.toString() : "";
        }
        return result != null ? result.toString() : "";
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif");
    }

    private void updateProgress(Long jobId, int progress, String status) {
        ParseJob job = new ParseJob();
        job.setId(jobId);
        job.setProgress(progress);
        job.setStatus(status);
        parseJobMapper.updateById(job);
    }
}
