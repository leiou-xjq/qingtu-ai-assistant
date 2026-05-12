package com.qingtu.agent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingtu.agent.entity.dto.CourseImportDTO;
import com.qingtu.agent.entity.po.CourseSchedule;
import com.qingtu.agent.mapper.CourseScheduleMapper;
import com.qingtu.agent.service.CourseCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCacheServiceImpl implements CourseCacheService {

    private static final String COURSE_KEY_PREFIX = "qtu:course:";
    private static final String EMPTY_MARKER = "EMPTY";
    private static final long CACHE_TTL = 30 * 24 * 60 * 60L; // 30天

    private final StringRedisTemplate redisTemplate;
    private final CourseScheduleMapper courseMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void saveCourses(Long userId, List<CourseImportDTO> courses) {
        try {
            String key = COURSE_KEY_PREFIX + userId;
            String json;
            if (courses == null || courses.isEmpty()) {
                json = EMPTY_MARKER;
            } else {
                json = objectMapper.writeValueAsString(courses);
            }
            redisTemplate.opsForValue().set(key, json, CACHE_TTL, TimeUnit.SECONDS);
            log.info("课程缓存已保存: userId={}, count={}", userId, courses != null ? courses.size() : 0);
        } catch (Exception e) {
            log.error("保存课程缓存失败: userId={}", userId, e);
        }
    }

    @Override
    public List<CourseImportDTO> getCourses(Long userId) {
        try {
            String key = COURSE_KEY_PREFIX + userId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty() || EMPTY_MARKER.equals(json)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<CourseImportDTO>>() {});
        } catch (Exception e) {
            log.error("获取课程缓存失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clearCourses(Long userId) {
        try {
            String key = COURSE_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            log.info("课程缓存已清除: userId={}", userId);
        } catch (Exception e) {
            log.error("清除课程缓存失败: userId={}", userId, e);
        }
    }

    @Override
    @Async
    public void syncToDatabase(Long userId) {
        List<CourseImportDTO> courses = getCourses(userId);
        if (courses == null || courses.isEmpty()) {
            log.info("同步课程到数据库: 无缓存数据, userId={}", userId);
            return;
        }

        log.info("开始异步同步课程到数据库: userId={}, count={}", userId, courses.size());
        int successCount = 0;
        int failCount = 0;

        for (CourseImportDTO dto : courses) {
            try {
                CourseSchedule existing = courseMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseSchedule>()
                                .eq(CourseSchedule::getUserId, userId)
                                .eq(CourseSchedule::getName, dto.getName())
                                .eq(CourseSchedule::getWeekday, dto.getWeekday())
                                .eq(CourseSchedule::getDeleted, 0)
                );

                if (existing == null) {
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
                        course.setStartTime(LocalTime.parse(dto.getStartTime()));
                    }
                    if (dto.getEndTime() != null && !dto.getEndTime().isEmpty()) {
                        course.setEndTime(LocalTime.parse(dto.getEndTime()));
                    }

                    courseMapper.insert(course);
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("同步课程失败: userId={}, course={}", userId, dto.getName(), e);
            }
        }

        log.info("课程同步完成: userId={}, success={}, fail={}", userId, successCount, failCount);
    }
}
