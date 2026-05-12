package com.qingtu.agent.application.service;

import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.api.sse.SSEHandler;
import com.qingtu.agent.application.event.ConversationEventPublisher;
import com.qingtu.agent.common.Constants;
import com.qingtu.agent.domain.tool.IntentClassifier;
import com.qingtu.agent.domain.tool.IntentType;
import com.qingtu.agent.infrastructure.redis.RedisSessionManager;
import com.qingtu.agent.infrastructure.redis.RedisSessionManager.ChatRecord;
import com.qingtu.agent.infrastructure.security.PromptSecurityFilter;
import com.qingtu.agent.infrastructure.security.PromptSecurityFilter.SecurityCheckResult;
import com.qingtu.agent.rag.RagServiceCore;
import com.qingtu.agent.exception.BusinessException;
import com.qingtu.agent.support.trace.TraceIdGenerator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIConversationService {

    private final RedisSessionManager sessionManager;
    private final PromptSecurityFilter securityFilter;
    private final IntentClassifier intentClassifier;
    private final RagServiceCore ragService;
    private final QingTuAgent agent;
    private final ConversationEventPublisher eventPublisher;
    private final SSEHandler sseHandler;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public void processStream(Long userId, String sessionId, String userMessage,
                           SseEmitter emitter, String traceId) {
        long startTime = System.currentTimeMillis();

        SecurityCheckResult securityResult = securityFilter.check(userMessage);
        if (!securityResult.passed()) {
            sseHandler.sendError(emitter, securityResult.reason());
            return;
        }

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("aiChat");
        if (!rateLimiter.acquirePermission(1)) {
            sseHandler.sendError(emitter, "请求过于频繁，请稍后再试");
            return;
        }

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = sessionManager.createSession(userId);
        } else {
            sessionManager.refreshTTL(userId, sessionId);
        }

        String finalSessionId = sessionId;

        sessionManager.appendMessage(userId, finalSessionId,
            RedisSessionManager.MessageRole.USER, userMessage);

        List<ChatRecord> history = sessionManager.getHistory(userId, finalSessionId);

        String ragContext = "";
        String skillContext = "";
        IntentType intent = intentClassifier.classify(userId, userMessage, new HashMap<>());

        if (intent == IntentType.RAG && skillContext.isBlank()) {
            ragContext = ragService.retrieveCommonContext(userMessage, 5);
        }

        if (!skillContext.isBlank()) {
            ragContext = skillContext;
        }

        String prompt = buildPrompt(userMessage, intent, ragContext, history);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProvider");
        AtomicReference<String> fullResponse = new AtomicReference<>("");

        try {
            Flux<String> stream = agent.chatStream(prompt);

            stream.subscribe(
                token -> {
                    fullResponse.updateAndGet(v -> v + token);
                    sseHandler.sendToken(emitter, token);
                },
                error -> {
                    log.error("流式输出错误: traceId={}, error={}", traceId, error.getMessage());
                    sseHandler.sendError(emitter, "AI服务暂时不可用");
                },
                () -> {
                    sseHandler.sendDone(emitter);

                    sessionManager.appendMessage(userId, finalSessionId,
                        RedisSessionManager.MessageRole.ASSISTANT, fullResponse.get());

                    eventPublisher.publishConversation(
                        userId, finalSessionId, userMessage,
                        fullResponse.get(), intent.name(),
                        null, System.currentTimeMillis() - startTime
                    );
                }
            );

        } catch (Exception e) {
            log.error("AI生成失败: traceId={}, error={}", traceId, e.getMessage());
            sseHandler.sendError(emitter, "AI服务暂时不可用，请稍后再试");
            eventPublisher.publishAlert(userId, "ERROR",
                "AI生成失败: " + e.getMessage(), traceId);
        }
    }

    private String buildPrompt(String userMessage, IntentType intent,
                             String ragContext, List<ChatRecord> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是青途智伴AI助手，专为大学生设计。\n\n");

        if (!history.isEmpty()) {
            prompt.append("【对话历史】\n");
            for (ChatRecord msg : history) {
                prompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("用户: ").append(userMessage).append("\n助手: ");
        return prompt.toString();
    }

    }