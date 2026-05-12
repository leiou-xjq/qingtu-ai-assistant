package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.ParseJob;
import com.qingtu.agent.service.ParseJobService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文档解析任务接口
 */
@RestController
@RequestMapping("/parse")
@RequiredArgsConstructor
public class ParseController {

    private final ParseJobService parseJobService;
    private final JwtUtil jwtUtil;

    /**
     * 创建解析任务
     * POST /api/parse/jobs
     */
    @PostMapping("/jobs")
    public CommonResult<?> createJob(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "file_type", defaultValue = "pdf") String fileType) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return CommonResult.fail("请先登录");
        }

        ParseJob job = parseJobService.createJob(userId, file, fileType);
        return CommonResult.success(Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "fileName", job.getFileName()
        ));
    }

    /**
     * 查询任务状态/进度/结果
     * GET /api/parse/jobs/{id}
     */
    @GetMapping("/jobs/{id}")
    public CommonResult<?> getJob(@PathVariable Long id) {
        ParseJob job = parseJobService.getJob(id);
        if (job == null) {
            return CommonResult.fail("任务不存在");
        }

        return CommonResult.success(Map.of(
                "jobId", job.getId(),
                "status", job.getStatus(),
                "progress", job.getProgress(),
                "fileName", job.getFileName(),
                "result", job.getResult(),
                "errorMessage", job.getErrorMessage(),
                "clarifyingQuestions", job.getClarifyingQuestions(),
                "createdAt", job.getCreatedAt(),
                "completedAt", job.getCompletedAt()
        ));
    }

    /**
     * 人工确认解析结果
     * POST /api/parse/jobs/{id}/confirm
     */
    @PostMapping("/jobs/{id}/confirm")
    public CommonResult<?> confirmJob(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> confirmedSchedules) {

        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return CommonResult.fail("请先登录");
        }

        try {
            parseJobService.confirmJob(id, confirmedSchedules);
            return CommonResult.success(Map.of(
                    "message", "确认成功，已保存 " + confirmedSchedules.size() + " 条课程"
            ));
        } catch (Exception e) {
            return CommonResult.fail(e.getMessage());
        }
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}
