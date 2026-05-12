package com.qingtu.agent.api.controller;

import com.qingtu.agent.api.dto.ChatRequest;
import com.qingtu.agent.api.sse.SSEHandler;
import com.qingtu.agent.application.service.AIConversationService;
import com.qingtu.agent.support.trace.TraceIdGenerator;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIChatController {

    private final AIConversationService conversationService;
    private final SSEHandler sseHandler;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request,
                                  HttpServletRequest httpRequest) {
        String traceId = request.getTraceId();
        if (traceId == null) {
            traceId = TraceIdGenerator.generate();
        }

        Long userId = extractUserId(httpRequest);
        SseEmitter emitter = sseHandler.createHandler(request.getSessionId());

        log.info("收到AI对话请求: userId={}, sessionId={}, traceId={}",
            userId, request.getSessionId(), traceId);

        if (userId == null) {
            sseHandler.sendError(emitter, "用户未登录");
            return emitter;
        }

        conversationService.processStream(
            userId,
            request.getSessionId(),
            request.getMessage(),
            emitter,
            traceId
        );

        return emitter;
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtUtil.getUserId(token);
            } catch (Exception e) {
                log.warn("JWT解析失败: {}", e.getMessage());
            }
        }
        return null;
    }
}
