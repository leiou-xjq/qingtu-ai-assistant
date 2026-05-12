package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.CourseProgressService;
import com.qingtu.agent.service.CourseService;
import com.qingtu.agent.service.NoteService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI笔记控制器
 */
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final CourseService courseService;
    private final NoteService noteService;
    private final CourseProgressService courseProgressService;
    private final JwtUtil jwtUtil;

    /**
     * 生成今日课程AI笔记
     */
    @GetMapping("/today")
    public CommonResult<?> generateTodayNotes(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return courseService.generateTodayNotes(userId);
    }

    /**
     * 获取笔记列表
     */
    @GetMapping("/list")
    public CommonResult<?> listNotes(HttpServletRequest request,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer weekNum,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getUserIdFromRequest(request);
        return noteService.listNotes(userId, courseId, weekNum, page, size);
    }

    /**
     * 获取笔记详情
     */
    @GetMapping("/{id}")
    public CommonResult<?> getNoteById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return noteService.getNoteById(userId, id);
    }

    /**
     * 获取课程的所有笔记
     */
    @GetMapping("/course/{courseId}")
    public CommonResult<?> getNotesByCourse(HttpServletRequest request, @PathVariable Long courseId) {
        Long userId = getUserIdFromRequest(request);
        return noteService.getNotesByCourse(userId, courseId);
    }

    /**
     * 分析课程进度
     */
    @GetMapping("/progress/{courseId}")
    public CommonResult<?> analyzeCourseProgress(HttpServletRequest request, @PathVariable Long courseId) {
        Long userId = getUserIdFromRequest(request);
        return courseProgressService.analyzeCourseProgress(userId, courseId);
    }

    /**
     * 生成指定课程的笔记
     */
    @PostMapping("/generate/{courseId}")
    public CommonResult<?> generateCourseNote(HttpServletRequest request,
            @PathVariable Long courseId,
            @RequestParam(required = false) Integer weekNum) {
        Long userId = getUserIdFromRequest(request);
        return courseProgressService.generateCourseNote(userId, courseId, weekNum);
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/{id}")
    public CommonResult<?> deleteNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return noteService.deleteNote(userId, id);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}