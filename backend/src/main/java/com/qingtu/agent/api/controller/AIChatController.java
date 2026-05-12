package com.qingtu.agent.api.controller;

import com.qingtu.agent.application.service.AIConversationService;
import com.qingtu.agent.api.dto.ChatRequest;
import com.qingtu.agent.api.sse.SSEHandler;
import com.qingtu.agent.support.trace.TraceIdGenerator;
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

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        String traceId = request.getTraceId();
        if (traceId == null) {
            traceId = TraceIdGenerator.generate();
        }

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
}