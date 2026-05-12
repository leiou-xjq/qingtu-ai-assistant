package com.qingtu.agent.service.impl;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.service.CourseService;
import com.qingtu.agent.service.CourseCacheService;
import com.qingtu.agent.service.DocumentParserService;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.entity.dto.CourseImportDTO;
import com.qingtu.agent.entity.dto.CourseDTO;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.task.WeekUtil;
import com.qingtu.agent.util.ExcelUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseScheduleMapper courseMapper;
    private final UserMapper userMapper;
    private final QingTuAgent qingTuAgent;
    private final DocumentParserService documentParserService;
    private final CourseCacheService courseCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void initWeekUtil(Long userId) {
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getSemesterStart() != null) {
                WeekUtil.setSemesterStart(user.getSemesterStart());
            } else {
                WeekUtil.setSemesterStart(LocalDate.of(2025, 3, 3));
            }
        } else {
            WeekUtil.setSemesterStart(LocalDate.of(2025, 3, 3));
        }
    }

    private void clearWeekUtil() {
        WeekUtil.clearSemesterStart();
    }

    @Override
    public CommonResult<?> getWeekSchedule(Long userId, Integer weekNum) {
        initWeekUtil(userId);
        try {
            if (weekNum == null || weekNum <= 0) {
                weekNum = WeekUtil.getCurrentWeek(null);
            }

            List<CourseImportDTO> cachedCourses = courseCacheService.getCourses(userId);
            Map<Integer, List<Map<String, Object>>> weekSchedule = new TreeMap<>();

            if (cachedCourses != null && !cachedCourses.isEmpty()) {
                for (int day = 1; day <= 7; day++) {
                    List<Map<String, Object>> dayCourses = new ArrayList<>();
                    for (CourseImportDTO course : cachedCourses) {
                        if (course.getWeekday() == day) {
                            Integer ws = course.getWeekStart() != null ? course.getWeekStart() : 1;
                            Integer we = course.getWeekEnd() != null ? course.getWeekEnd() : 20;
                            if (ws <= weekNum && we >= weekNum) {
                                Map<String, Object> m = new HashMap<>();
                                m.put("id", 0);
                                m.put("name", course.getName());
                                m.put("teacher", course.getTeacher());
                                m.put("location", course.getLocation());
                                m.put("startTime", course.getStartTime());
                                m.put("endTime", course.getEndTime());
                                m.put("weekStart", ws);
                                m.put("weekEnd", we);
                                m.put("weekday", course.getWeekday());
                                m.put("courseType", "required");
                                m.put("color", "#3B82F6");
                                m.put("reminderEnabled", 1);
                                m.put("reminderMinutes", 15);
                                dayCourses.add(m);
                            }
                        }
                    }
                    dayCourses.sort((a, b) -> ((String) a.get("startTime")).compareTo((String) b.get("startTime")));
                    weekSchedule.put(day, dayCourses);
                }
            } else {
                for (int day = 1; day <= 7; day++) {
                    List<CourseSchedule> courses = courseMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                                    .eq(CourseSchedule::getUserId, userId)
                                    .eq(CourseSchedule::getWeekday, day)
                                    .le(CourseSchedule::getWeekStart, weekNum)
                                    .ge(CourseSchedule::getWeekEnd, weekNum)
                                    .eq(CourseSchedule::getDeleted, 0)
                                    .orderByAsc(CourseSchedule::getStartTime)
                    );

                    weekSchedule.put(day, courses.stream().map(c -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", c.getId());
                        m.put("name", c.getName());
                        m.put("teacher", c.getTeacher());
                        m.put("location", c.getLocation());
                        m.put("startTime", c.getStartTime().toString());
                        m.put("endTime", c.getEndTime().toString());
                        m.put("weekStart", c.getWeekStart() != null ? c.getWeekStart() : 1);
                        m.put("weekEnd", c.getWeekEnd() != null ? c.getWeekEnd() : 20);
                        m.put("weekday", c.getWeekday());
                        m.put("courseType", c.getCourseType());
                        m.put("color", c.getColor());
                        m.put("reminderEnabled", c.getReminderEnabled());
                        m.put("reminderMinutes", c.getReminderMinutes());
                        return m;
                    }).toList());
                }
            }

            return CommonResult.success(Map.of(
                    "weekNum", weekNum,
                    "weekDateRange", WeekUtil.getWeekDateRange(null, weekNum),
                    "schedule", weekSchedule
            ));
        } finally {
            clearWeekUtil();
        }
    }

    @Override
    public CommonResult<?> getTodayCourses(Long userId) {
        initWeekUtil(userId);
        try {
            int currentWeek = WeekUtil.getCurrentWeek(null);
            int weekday = WeekUtil.getDayOfWeek();

            List<CourseImportDTO> cachedCourses = courseCacheService.getCourses(userId);

            if (cachedCourses != null && !cachedCourses.isEmpty()) {
                List<Map<String, Object>> todayCourses = new ArrayList<>();
                for (CourseImportDTO course : cachedCourses) {
                    if (course.getWeekday() == weekday) {
                        Integer ws = course.getWeekStart() != null ? course.getWeekStart() : 1;
                        Integer we = course.getWeekEnd() != null ? course.getWeekEnd() : 20;
                        if (ws <= currentWeek && we >= currentWeek) {
                            Map<String, Object> m = new HashMap<>();
                            m.put("id", 0);
                            m.put("name", course.getName());
                            m.put("teacher", course.getTeacher());
                            m.put("location", course.getLocation());
                            m.put("startTime", course.getStartTime());
                            m.put("endTime", course.getEndTime());
                            m.put("color", "#3B82F6");
                            todayCourses.add(m);
                        }
                    }
                }
                todayCourses.sort((a, b) -> ((String) a.get("startTime")).compareTo((String) b.get("startTime")));
                return CommonResult.success(todayCourses);
            }

            List<CourseSchedule> courses = courseMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getUserId, userId)
                            .eq(CourseSchedule::getWeekday, weekday)
                            .le(CourseSchedule::getWeekStart, currentWeek)
                            .ge(CourseSchedule::getWeekEnd, currentWeek)
                            .eq(CourseSchedule::getDeleted, 0)
                            .orderByAsc(CourseSchedule::getStartTime)
            );

            List<Map<String, Object>> result = courses.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                m.put("teacher", c.getTeacher());
                m.put("location", c.getLocation());
                m.put("startTime", c.getStartTime() != null ? c.getStartTime().toString() : "");
                m.put("endTime", c.getEndTime() != null ? c.getEndTime().toString() : "");
                m.put("color", c.getColor());
                return m;
            }).toList();

            return CommonResult.success(result);
        } finally {
            clearWeekUtil();
        }
    }

    @Override
    public CommonResult<?> generateTodayNotes(Long userId) {
        initWeekUtil(userId);
        try {
            int currentWeek = WeekUtil.getCurrentWeek(null);
            int weekday = WeekUtil.getDayOfWeek();
            List<CourseSchedule> courses = courseMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getUserId, userId)
                            .eq(CourseSchedule::getWeekday, weekday)
                            .eq(CourseSchedule::getDeleted, 0)
            );

            if (courses == null || courses.isEmpty()) {
                return CommonResult.fail("今日无课程");
            }

            StringBuilder courseInfo = new StringBuilder();
            courseInfo.append("当前学期：第").append(currentWeek).append("周\n\n");

            for (CourseSchedule course : courses) {
                int courseWeekStart = course.getWeekStart() != null ? course.getWeekStart() : 1;
                int courseWeekEnd = course.getWeekEnd() != null ? course.getWeekEnd() : 20;
                int totalCourseWeeks = courseWeekEnd - courseWeekStart + 1;
                int weeksPassed = currentWeek - courseWeekStart + 1;
                if (weeksPassed < 1) weeksPassed = 1;
                if (weeksPassed > totalCourseWeeks) weeksPassed = totalCourseWeeks;
                double progressPercent = (double) weeksPassed / totalCourseWeeks * 100;

                courseInfo.append("【").append(course.getName()).append("】");
                courseInfo.append(" 地点：").append(course.getLocation());
                courseInfo.append(" 时间：").append(course.getStartTime()).append("-").append(course.getEndTime());
                courseInfo.append(" 课程进度：约").append(String.format("%.0f", progressPercent)).append("%");
                courseInfo.append("\n");
            }

            String prompt = "我今天上了以下课程（当前为第" + currentWeek + "周）：\n" + courseInfo.toString() +
                    "请根据今日课程内容和课程进度生成学习笔记，要求：\n" +
                    "1. 分析当前教学阶段（基础/进阶/冲刺）\n" +
                    "2. 今日课程内容总结\n" +
                    "3. 核心知识点（每个课程2-3个）\n" +
                    "4. 课堂重点\n" +
                    "5. 针对当前进度的复习建议\n\n" +
                    "请用Markdown格式输出";

            String notes = qingTuAgent.chat(prompt);

            Map<String, Object> result = new HashMap<>();
            result.put("currentWeek", currentWeek);
            result.put("courses", courses.stream().map(c -> c.getName()).toList());
            result.put("notes", notes);

            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("AI笔记生成失败", e);
            return CommonResult.fail("笔记生成失败：" + e.getMessage());
        } finally {
            clearWeekUtil();
        }
    }

    @Override
    public CommonResult<?> addCourse(Long userId, Object dto) {
        CourseDTO courseDTO = objectMapper.convertValue(dto, CourseDTO.class);
        CourseSchedule course = new CourseSchedule();
        course.setUserId(userId);
        course.setName(courseDTO.getName());
        course.setTeacher(courseDTO.getTeacher());
        course.setLocation(courseDTO.getLocation());
        course.setWeekday(courseDTO.getWeekday());
        if (courseDTO.getStartTime() != null && !courseDTO.getStartTime().isEmpty()) {
            course.setStartTime(LocalTime.parse(courseDTO.getStartTime()));
        }
        if (courseDTO.getEndTime() != null && !courseDTO.getEndTime().isEmpty()) {
            course.setEndTime(LocalTime.parse(courseDTO.getEndTime()));
        }
        course.setWeekStart(courseDTO.getWeekStart() != null ? courseDTO.getWeekStart() : 1);
        course.setWeekEnd(courseDTO.getWeekEnd() != null ? courseDTO.getWeekEnd() : 20);
        course.setCourseType(courseDTO.getCourseType());
        course.setReminderEnabled(courseDTO.getReminderEnabled() != null ? courseDTO.getReminderEnabled() : 1);
        course.setReminderMinutes(courseDTO.getReminderMinutes() != null ? courseDTO.getReminderMinutes() : 15);
        courseMapper.insert(course);
        return CommonResult.success("课程添加成功");
    }

    @Override
    public CommonResult<?> updateCourse(Long userId, Long courseId, Object dto) {
        CourseDTO courseDTO = objectMapper.convertValue(dto, CourseDTO.class);
        
        if (courseId == null || courseId == 0) {
            CourseSchedule existing = courseMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getUserId, userId)
                            .eq(CourseSchedule::getName, courseDTO.getName())
                            .eq(CourseSchedule::getWeekday, courseDTO.getWeekday())
                            .eq(CourseSchedule::getDeleted, 0)
            );
            
            if (existing != null) {
                courseId = existing.getId();
            } else {
                return addCourse(userId, dto);
            }
        }
        
        CourseSchedule course = courseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getId, courseId)
                        .eq(CourseSchedule::getUserId, userId));
        if (course == null) return CommonResult.fail("课程不存在");

        if (courseDTO.getName() != null) course.setName(courseDTO.getName());
        if (courseDTO.getTeacher() != null) course.setTeacher(courseDTO.getTeacher());
        if (courseDTO.getLocation() != null) course.setLocation(courseDTO.getLocation());
        if (courseDTO.getWeekday() != null) course.setWeekday(courseDTO.getWeekday());
        if (courseDTO.getStartTime() != null && !courseDTO.getStartTime().isEmpty()) {
            course.setStartTime(LocalTime.parse(courseDTO.getStartTime()));
        }
        if (courseDTO.getEndTime() != null && !courseDTO.getEndTime().isEmpty()) {
            course.setEndTime(LocalTime.parse(courseDTO.getEndTime()));
        }
        if (courseDTO.getWeekStart() != null) course.setWeekStart(courseDTO.getWeekStart());
        if (courseDTO.getWeekEnd() != null) course.setWeekEnd(courseDTO.getWeekEnd());
        if (courseDTO.getCourseType() != null) course.setCourseType(courseDTO.getCourseType());
        if (courseDTO.getReminderEnabled() != null) course.setReminderEnabled(courseDTO.getReminderEnabled());
        if (courseDTO.getReminderMinutes() != null) course.setReminderMinutes(courseDTO.getReminderMinutes());
        courseMapper.updateById(course);
        return CommonResult.success("课程更新成功");
    }

    @Override
    public CommonResult<?> deleteCourse(Long userId, Long courseId) {
        courseMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getId, courseId)
                .eq(CourseSchedule::getUserId, userId));
        return CommonResult.success("删除成功");
    }

    @Override
    public CommonResult<?> importSchedule(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return CommonResult.fail("请上传课表文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) return CommonResult.fail("文件名无效");

        String lowerName = filename.toLowerCase();

        // Excel 分支：保留原有 EasyExcel 解析
        if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return importFromExcel(userId, file);
        }

        // PDF/Word/TXT 分支：AI 智能解析
        if (lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt")) {
            return importFromDocument(userId, file);
        }

        return CommonResult.fail("不支持的文件格式，请上传Excel/PDF/Word/TXT文件");
    }

    /** Excel 导入（原有逻辑） */
    private CommonResult<?> importFromExcel(Long userId, MultipartFile file) {
        try {
            List<CourseImportDTO> courses = ExcelUtil.readExcel(file.getInputStream(), CourseImportDTO.class);
            if (courses == null || courses.isEmpty()) return CommonResult.fail("课表内容为空");
            return insertCourses(userId, courses);
        } catch (Exception e) {
            return CommonResult.fail("解析Excel失败：" + e.getMessage());
        }
    }

    /** AI 智能导入（PDF/Word/TXT） */
    private CommonResult<?> importFromDocument(Long userId, MultipartFile file) {
        try {
            List<String> chunks = documentParserService.parseDocument(file);
            String rawText = String.join("\n", chunks);
            if (rawText.isBlank()) return CommonResult.fail("文档内容为空");

            String prompt = "你是一个课程表解析助手。请将以下课表文本解析为JSON数组。\n"
                    + "解析规则：\n"
                    + "1. 每门课程只生成一条记录\n"
                    + "2. 如果上课时间跨越多行（如第一节课08:00-09:40在第一行，第二节课10:00-11:40在第二行），\n"
                    + "   应该合并为一个时间字段，使用\"08:00-09:40,10:00-11:40\"格式\n"
                    + "3. 时间格式：开始时间用startTime，结束时间用endTime\n"
                    + "4. 如果有多个时段，先取第一个时段的开始时间作为startTime，最后一个时段的结束时间作为endTime\n\n"
                    + "每个课程包含字段：\n"
                    + "- name: 课程名称\n"
                    + "- teacher: 授课教师\n"
                    + "- location: 上课地点\n"
                    + "- weekday: 星期几(1-7,1表示周一)\n"
                    + "- startTime: 开始时间(HH:mm格式，取第一节课开始时间)\n"
                    + "- endTime: 结束时间(HH:mm格式，取最后一节课结束时间)\n"
                    + "- weekStart: 开始周(数字)\n"
                    + "- weekEnd: 结束周(数字)\n\n"
                    + "课表文本：\n" + rawText + "\n\n"
                    + "请只返回JSON数组，不要其他内容。示例格式：[{\"name\":\"高等数学\",\"teacher\":\"张三\","
                    + "\"location\":\"教一楼301\",\"weekday\":1,\"startTime\":\"08:00\",\"endTime\":\"09:40\",\"weekStart\":1,\"weekEnd\":16}]";

            String jsonResponse = qingTuAgent.chat(prompt);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                return CommonResult.fail("AI解析失败，请检查课表文档内容");
            }

            int startIdx = jsonResponse.indexOf('[');
            int endIdx = jsonResponse.lastIndexOf(']');
            if (startIdx >= 0 && endIdx > startIdx) {
                jsonResponse = jsonResponse.substring(startIdx, endIdx + 1);
            }

            CourseImportDTO[] coursesArray = objectMapper.readValue(jsonResponse, CourseImportDTO[].class);
            List<CourseImportDTO> courses = Arrays.asList(coursesArray);

            courseCacheService.saveCourses(userId, courses);
            courseCacheService.syncToDatabase(userId);

            return CommonResult.success(Map.of(
                    "message", "导入成功",
                    "count", courses.size(),
                    "source", "redis"
            ));

        } catch (Exception e) {
            log.error("AI导入失败", e);
            return CommonResult.fail("AI解析失败：" + e.getMessage());
        }
    }

    /** 公共插入逻辑 */
    private CommonResult<?> insertCourses(Long userId, List<CourseImportDTO> courses) {
        if (courses == null || courses.isEmpty()) {
            return CommonResult.fail("课程列表为空");
        }

        int successCount = 0, failCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < courses.size(); i++) {
            try {
                CourseImportDTO dto = courses.get(i);
                if (dto == null) {
                    failCount++;
                    errors.add("第" + (i + 1) + "条：数据为空");
                    continue;
                }
                if (dto.getName() == null || dto.getName().isEmpty()) {
                    failCount++;
                    errors.add("第" + (i + 1) + "条：课程名称为空");
                    continue;
                }

                CourseSchedule course = new CourseSchedule();
                course.setUserId(userId);
                course.setName(dto.getName());
                course.setLocation(dto.getLocation());
                course.setWeekday(dto.getWeekday());
                course.setTeacher(dto.getTeacher());
                course.setWeekStart(dto.getWeekStart() != null ? dto.getWeekStart() : 1);
                course.setWeekEnd(dto.getWeekEnd() != null ? dto.getWeekEnd() : 20);
                course.setReminderEnabled(1);
                course.setReminderMinutes(15);

                if (dto.getStartTime() != null && !dto.getStartTime().isEmpty()) {
                    try {
                        course.setStartTime(LocalTime.parse(dto.getStartTime()));
                    } catch (Exception e) {
                        log.warn("解析开始时间失败: {}", dto.getStartTime());
                    }
                }
                if (dto.getEndTime() != null && !dto.getEndTime().isEmpty()) {
                    try {
                        course.setEndTime(LocalTime.parse(dto.getEndTime()));
                    } catch (Exception e) {
                        log.warn("解析结束时间失败: {}", dto.getEndTime());
                    }
                }

                courseMapper.insert(course);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("第" + (i + 1) + "条：" + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        result.put("message", failCount > 0 ? "部分数据导入失败" : "导入成功");

        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> downloadTemplate() {
        return CommonResult.success("请使用标准课表模板，包含列：课程名称、上课地点、星期、开始时间、结束时间、开始周、结束周");
    }

    @Override
    public CommonResult<?> setCourseReminder(Long userId, Long courseId, boolean enabled, int minutes) {
        CourseSchedule course = courseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getId, courseId)
                        .eq(CourseSchedule::getUserId, userId));
        if (course == null) {
            return CommonResult.fail(ResultCode.COURSE_NOT_FOUND);
        }
        course.setReminderEnabled(enabled ? 1 : 0);
        course.setReminderMinutes(minutes);
        courseMapper.updateById(course);
        return CommonResult.success("设置成功");
    }
}