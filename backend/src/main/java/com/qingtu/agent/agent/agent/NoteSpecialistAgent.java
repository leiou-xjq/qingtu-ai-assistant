package com.qingtu.agent.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.config.DashScopeConfig;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.Notes;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.NotesMapper;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.task.WeekUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteSpecialistAgent {

    private final NotesMapper notesMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final UserMapper userMapper;
    private final DashScopeConfig dashScopeConfig;

    private static final int MAX_RETRIES = 3;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    private final RestTemplate restTemplate = createConfiguredRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static RestTemplate createConfiguredRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    public ResultMessage execute(String action, UserContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String taskId = java.util.UUID.randomUUID().toString();
        String correlationId = params.getOrDefault("_correlationId", "").toString();

        try {
            return switch (action.toLowerCase()) {
                case "generate" -> generateNote(taskId, context, params, correlationId);
                case "query" -> queryNote(taskId, context, params, correlationId);
                default -> ResultMessage.failure(taskId, "note", action, "未知动作: " + action, correlationId, context.getUserId());
            };
        } catch (Exception e) {
            log.error("笔记操作失败", e);
            return ResultMessage.failure(taskId, "note", action, e.getMessage(), correlationId, context.getUserId());
        }
    }

    private ResultMessage generateNote(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        String noteType = params.getOrDefault("type", "note").toString();
        boolean includeTodayCourses = Boolean.parseBoolean(params.getOrDefault("includeTodayCourses", "false").toString());

        List<CourseSchedule> todayCourses = new ArrayList<>();

        if (includeTodayCourses) {
            todayCourses = getTodayCourses(context);
        }

        List<Map<String, Object>> results = new ArrayList<>();

        if (todayCourses.isEmpty()) {
            String userInput = params.getOrDefault("userInput", params.getOrDefault("content", "")).toString();
            String generatedContent = generateContentWithRetry(noteType, userInput, "", null, context);
            String title = generateTitle(noteType, LocalDate.now().toString(), null);

            Notes note = new Notes();
            note.setUserId(context.getUserId());
            note.setTitle(title);
            note.setContent(generatedContent);
            note.setNoteType(noteType);
            note.setCreateTime(LocalDateTime.now());
            note.setUpdateTime(LocalDateTime.now());
            notesMapper.insert(note);

            results.add(Map.of("noteId", note.getId(), "title", title));
        } else {
            for (CourseSchedule course : todayCourses) {
                String courseInfo = buildCourseInfo(course);
                String userInput = params.getOrDefault("userInput", params.getOrDefault("content", "")).toString();
                String generatedContent = generateContentWithRetry(noteType, userInput, courseInfo, course.getName(), context);
                String title = generateTitle(noteType, LocalDate.now().toString(), course.getName());

                Notes note = new Notes();
                note.setUserId(context.getUserId());
                note.setTitle(title);
                note.setContent(generatedContent);
                note.setNoteType(noteType);
                note.setCourseId(course.getId());
                note.setCreateTime(LocalDateTime.now());
                note.setUpdateTime(LocalDateTime.now());
                notesMapper.insert(note);

                results.add(Map.of("noteId", note.getId(), "title", title, "courseName", course.getName()));
            }
        }

        log.info("笔记生成成功: userId={}, count={}", context.getUserId(), results.size());

        return ResultMessage.success(taskId, "note", "generate",
                Map.of("notes", results, "count", results.size()),
                correlationId, context.getUserId());
    }

    private String buildCourseInfo(CourseSchedule course) {
        StringBuilder sb = new StringBuilder();
        sb.append("【课程信息】\n");
        sb.append("- 课程名称：").append(course.getName()).append("\n");
        if (course.getLocation() != null) {
            sb.append("- 上课地点：").append(course.getLocation()).append("\n");
        }
        if (course.getStartTime() != null && course.getEndTime() != null) {
            sb.append("- 上课时间：").append(course.getStartTime()).append("-").append(course.getEndTime()).append("\n");
        }
        return sb.toString();
    }

    private String generateContentWithRetry(String noteType, String userInput, String courseInfo, String courseName, UserContext context) {
        String lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("LLM生成笔记，第{}次尝试", attempt);
                return generateContentWithLLM(noteType, userInput, courseInfo, context);
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("LLM生成笔记失败(第{}次): {}", attempt, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        int sleepTime = 1000 * attempt;
                        log.info("等待{}ms后重试...", sleepTime);
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.warn("LLM生成笔记全部失败，使用模板生成: {}", lastError);
        return generateFallbackContent(noteType, courseName, courseInfo);
    }

    private List<CourseSchedule> getTodayCourses(UserContext context) {
        try {
            User user = userMapper.selectById(context.getUserId());
            if (user != null && user.getSemesterStart() != null) {
                WeekUtil.setSemesterStart(user.getSemesterStart());
            } else {
                WeekUtil.setSemesterStart(LocalDate.of(2026, 3, 2));
            }

            int currentWeek = WeekUtil.getCurrentWeek(null);
            int weekday = WeekUtil.getDayOfWeek();

            List<CourseSchedule> courses = courseScheduleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getUserId, context.getUserId())
                            .eq(CourseSchedule::getWeekday, weekday)
                            .le(CourseSchedule::getWeekStart, currentWeek)
                            .ge(CourseSchedule::getWeekEnd, currentWeek)
                            .eq(CourseSchedule::getDeleted, 0)
                            .orderByAsc(CourseSchedule::getStartTime)
            );

            return courses != null ? courses : new ArrayList<>();

        } catch (Exception e) {
            log.error("获取今日课程信息失败", e);
            return new ArrayList<>();
        } finally {
            WeekUtil.clearSemesterStart();
        }
    }

    private String generateContentWithLLM(String noteType, String userInput, String courseInfo, UserContext context) throws Exception {
        String prompt = buildNotePrompt(noteType, userInput, courseInfo);

        String url = dashScopeConfig.getBaseUrl() + "/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", dashScopeConfig.getModel());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("enable_search", false);
        body.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dashScopeConfig.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(url, entity, String.class);

        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            String content = choices.get(0).path("message").path("content").asText();
            if (content != null && !content.isBlank()) {
                return content;
            }
        }
        throw new RuntimeException("LLM返回内容为空");
    }

    private String buildNotePrompt(String noteType, String userInput, String courseInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是学习笔记助手，根据课程信息生成Markdown格式的学习笔记。\n\n");

        if (courseInfo != null && !courseInfo.isEmpty()) {
            prompt.append(courseInfo).append("\n");
        }

        prompt.append("【要求】生成简洁的笔记，包括：\n");
        prompt.append("1. 课程名称和基本信息\n");
        prompt.append("2. 主要知识点（2-3个）\n");
        prompt.append("3. 课堂小结\n");
        prompt.append("4. 复习建议\n\n");

        if (courseInfo != null && courseInfo.contains("【课程信息】")) {
            prompt.append("请根据以上课程信息生成学习笔记。\n");
        }

        return prompt.toString();
    }

    private String generateFallbackContent(String noteType, String courseName, String courseInfo) {
        StringBuilder sb = new StringBuilder();
        String dateStr = LocalDate.now().toString();

        sb.append("# ").append(dateStr).append(" 学习笔记\n\n");

        if (courseInfo != null && courseInfo.contains("【课程信息】")) {
            sb.append("## ").append(courseName).append("\n\n");
            sb.append("**上课地点**：待补充\n\n");
            sb.append("**知识点**：\n");
            sb.append("- [ ] 请根据上课内容填写\n\n");
            sb.append("**课堂小结**：\n");
            sb.append("- [ ] 请根据上课内容填写\n\n");

            sb.append("## 复习建议\n\n");
            sb.append("1. 整理今日课程笔记\n");
            sb.append("2. 完成课后作业\n");
            sb.append("3. 预习下次课程内容\n\n");

            sb.append("> ⚠️ 这是基础模板，LLM服务暂时不可用，请稍后手动补充或重试生成。\n");
        } else {
            sb.append("## 知识点\n\n- [ ] 请填写学习内容\n\n");
            sb.append("## 总结\n\n请整理今日所学内容。\n\n");
            sb.append("## 复习计划\n\n- [ ] 制定复习计划\n");
        }

        return sb.toString();
    }

    private String generateTitle(String noteType, String dateStr, String courseName) {
        String prefix = switch (noteType.toLowerCase()) {
            case "summary" -> "课程总结";
            case "note" -> "学习笔记";
            case "review" -> "复习提纲";
            default -> "学习笔记";
        };

        if (courseName != null && !courseName.isEmpty()) {
            return courseName + " - " + dateStr;
        }

        return dateStr + " - " + prefix;
    }

    private ResultMessage queryNote(String taskId, UserContext context, Map<String, Object> params, String correlationId) {
        String courseName = params.getOrDefault("courseName", "").toString();

        List<Notes> notes = notesMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notes>()
                        .eq(Notes::getUserId, context.getUserId())
                        .like(!courseName.isBlank(), Notes::getTitle, courseName)
                        .orderByDesc(Notes::getCreateTime)
                        .last("LIMIT 10")
        );

        List<Map<String, Object>> noteList = new ArrayList<>();
        for (Notes n : notes) {
            Map<String, Object> map = new HashMap<>();
            map.put("noteId", n.getId());
            map.put("title", n.getTitle() != null ? n.getTitle() : "");
            map.put("type", n.getNoteType() != null ? n.getNoteType() : "note");
            map.put("createTime", n.getCreateTime() != null ? n.getCreateTime().toString() : "");
            noteList.add(map);
        }

        return ResultMessage.success(taskId, "note", "query",
                Map.of("notes", noteList, "count", noteList.size()),
                correlationId, context.getUserId());
    }
}
