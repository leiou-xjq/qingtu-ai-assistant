package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.CourseService;
import com.qingtu.agent.entity.dto.CourseDTO;
import com.qingtu.agent.entity.dto.CourseReminderDTO;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 课程管理控制器
 * 
 * 提供课程表管理、Excel导入、提醒设置等功能
 * 
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final JwtUtil jwtUtil;

    /**
     * 获取周课表
     */
    @GetMapping("/schedule")
    public CommonResult<?> getWeekSchedule(HttpServletRequest request,
                                           @RequestParam(required = false) Integer weekNum) {
        Long userId = getUserIdFromRequest(request);
        return courseService.getWeekSchedule(userId, weekNum);
    }

    /**
     * 获取今日课程
     */
    @GetMapping("/today")
    public CommonResult<?> getTodayCourses(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return courseService.getTodayCourses(userId);
    }

    /**
     * 添加课程
     */
    @PostMapping("/add")
    public CommonResult<?> addCourse(HttpServletRequest request, @RequestBody CourseDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return courseService.addCourse(userId, dto);
    }

    /**
     * 更新课程
     */
    @PutMapping("/{id}")
    public CommonResult<?> updateCourse(HttpServletRequest request,
                                         @PathVariable Long id,
                                         @RequestBody CourseDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return courseService.updateCourse(userId, id, dto);
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/{id}")
    public CommonResult<?> deleteCourse(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return courseService.deleteCourse(userId, id);
    }

    /**
     * Excel导入课表
     */
    @PostMapping("/import")
    public CommonResult<?> importSchedule(HttpServletRequest request,
                                          @RequestParam MultipartFile file) {
        Long userId = getUserIdFromRequest(request);
        return courseService.importSchedule(userId, file);
    }

    /**
     * 下载课表模板
     */
    @GetMapping("/template")
    public CommonResult<?> downloadTemplate() {
        return courseService.downloadTemplate();
    }

    /**
     * 设置课程提醒
     */
    @PutMapping("/{id}/reminder")
    public CommonResult<?> setCourseReminder(HttpServletRequest request,
                                              @PathVariable Long id,
                                              @RequestBody CourseReminderDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return courseService.setCourseReminder(userId, id, dto.isEnabled(), dto.getMinutes());
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}