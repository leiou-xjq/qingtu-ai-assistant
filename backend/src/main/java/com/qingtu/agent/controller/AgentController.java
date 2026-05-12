package com.qingtu.agent.controller;

import com.qingtu.agent.agent.orchestrator.OrchestratorAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Agent 对话控制器
 * 支持对话 + 文件上传
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent 对话", description = "多Agent智能对话接口")
public class AgentController {

    private final OrchestratorAgent orchestratorAgent;

    @PostMapping("/chat")
    @Operation(summary = "发送消息", description = "支持文本消息和文件上传")
    public Map<String, Object> chat(
            @RequestHeader("Authorization") String token,
            @RequestParam("message") String message,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, String> fileMap = new HashMap<>();

            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
                        fileMap.put(file.getOriginalFilename(), base64);
                        log.debug("上传文件: name={}, size={}", file.getOriginalFilename(), file.getSize());
                    }
                }
            }

            String response = orchestratorAgent.process(message, token, fileMap);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Agent对话完成: 耗时={}ms", duration);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", response);
            result.put("duration", duration);

            return result;

        } catch (Exception e) {
            log.error("Agent对话失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "处理失败：" + e.getMessage());
            return result;
        }
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态")
    public Map<String, Object> getTaskStatus(@PathVariable String taskId) {
        return orchestratorAgent.getTaskStatus(taskId);
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}