package com.qingtu.agent.api.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SSEHandler {

    public SseEmitter createHandler(String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onCompletion(() -> log.info("SSE连接完成: sessionId={}", sessionId));
        emitter.onTimeout(() -> log.warn("SSE连接超时: sessionId={}", sessionId));
        emitter.onError(e -> log.error("SSE错误: sessionId={}, error={}", sessionId, e.getMessage()));

        return emitter;
    }

    public void send(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("SSE发送失败: event={}, error={}", event, e.getMessage());
        }
    }

    public void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event()
                .name("token")
                .data(token));
        } catch (IOException e) {
            log.error("SSE发送Token失败: error={}", e.getMessage());
        }
    }

    public void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                .name("done")
                .data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            log.error("SSE完成失败: error={}", e.getMessage());
        }
    }

    public void sendError(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data(errorMessage));
            emitter.completeWithError(new RuntimeException(errorMessage));
        } catch (IOException e) {
            log.error("SSE发送错误失败: error={}", e.getMessage());
        }
    }
}