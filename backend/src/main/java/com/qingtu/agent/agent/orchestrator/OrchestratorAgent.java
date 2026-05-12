package com.qingtu.agent.agent.orchestrator;

import com.qingtu.agent.agent.agent.WeatherSpecialistAgent;
import com.qingtu.agent.agent.agent.ExpenseSpecialistAgent;
import com.qingtu.agent.agent.agent.CourseSpecialistAgent;
import com.qingtu.agent.agent.agent.ProfileSpecialistAgent;
import com.qingtu.agent.agent.agent.NoteSpecialistAgent;
import com.qingtu.agent.agent.agent.CalorieSpecialistAgent;
import com.qingtu.agent.agent.agent.ChatSpecialistAgent;
import com.qingtu.agent.agent.agent.SearchSpecialistAgent;
import com.qingtu.agent.agent.context.UserContext;
import com.qingtu.agent.agent.context.UserContextProvider;
import com.qingtu.agent.agent.message.AgentMessagePublisher;
import com.qingtu.agent.agent.message.ResultMessage;
import com.qingtu.agent.agent.message.TaskMessage;
import com.qingtu.agent.agent.message.config.AgentQueueConfig;
import com.qingtu.agent.agent.fallback.FallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * 编排器 Agent
 * 核心调度器：意图分析 → 任务分解 → 并行执行 → 结果聚合
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestratorAgent {

    private final IntentAnalyzer intentAnalyzer;
    private final ResultAggregator resultAggregator;
    private final UserContextProvider userContextProvider;
    private final AgentMessagePublisher messagePublisher;
    private final FallbackHandler fallbackHandler;
    private final ReActExecutor reActExecutor;

    private final WeatherSpecialistAgent weatherAgent;
    private final ExpenseSpecialistAgent expenseAgent;
    private final CourseSpecialistAgent courseAgent;
    private final ProfileSpecialistAgent profileAgent;
    private final NoteSpecialistAgent noteAgent;
    private final CalorieSpecialistAgent calorieAgent;
    private final ChatSpecialistAgent chatAgent;
    private final SearchSpecialistAgent searchAgent;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Map<String, CompletableFuture<ResultMessage>> pendingResults = new ConcurrentHashMap<>();

    private static final int RESULT_TIMEOUT_SECONDS = 30;

    public String process(String message, String token, Map<String, String> files) {
        return process(message, token, files, "");
    }

    public String process(String message, String token, Map<String, String> files, String sessionId) {
        long startTime = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();

        log.info("Orchestrator处理请求: message={}, correlationId={}", message.substring(0, Math.min(50, message.length())), correlationId);

        UserContext context = userContextProvider.getContext(token);
        if (context == null) {
            return "无法获取用户信息，请重新登录";
        }

        log.debug("用户上下文: userId={}, city={}, school={}", context.getUserId(), context.getCity(), context.getSchool());

        List<Task> tasks = intentAnalyzer.analyze(message, context, files);
        log.debug("意图分析结果: 共{}个任务", tasks.size());

        if (tasks.isEmpty()) {
            log.info("意图分析无结果，尝试ReAct推理模式");
            try {
                return reActExecutor.execute(message, context, sessionId, files);
            } catch (Exception e) {
                log.warn("ReAct执行失败，返回兜底: {}", e.getMessage());
                return "抱歉，我暂时无法理解您的问题，请换个说法试试？";
            }
        }

        if (tasks.size() == 1) {
            Task task = tasks.get(0);
            switch (task.getAgent().toLowerCase()) {
                case "chat" -> {
                    Map<String, Object> chatParams = new HashMap<>(task.getParameters());
                    chatParams.put("sessionId", sessionId);
                    ResultMessage result = chatAgent.execute("chat", context, chatParams);
                    return result.isSuccess() ? String.valueOf(result.getResult()) : "对话处理失败";
                }
                case "search" -> {
                    ResultMessage result = searchAgent.execute("search", context, task.getParameters());
                    return result.isSuccess() ? String.valueOf(result.getResult()) : "搜索处理失败";
                }
            }
        }

        List<CompletableFuture<ResultMessage>> futures = new ArrayList<>();

        for (Task task : tasks) {
            task.getParameters().put("sessionId", sessionId);
            CompletableFuture<ResultMessage> future = executeTaskAsync(task, context, files, correlationId);
            futures.add(future);
        }

        List<ResultMessage> results = waitForResults(futures, correlationId);

        String response = resultAggregator.aggregate(results, message);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Orchestrator处理完成: correlationId={}, 任务数={}, 耗时={}ms", correlationId, tasks.size(), duration);

        return response;
    }

    private CompletableFuture<ResultMessage> executeTaskAsync(Task task, UserContext context, Map<String, String> files, String correlationId) {
        CompletableFuture<ResultMessage> future = new CompletableFuture<>();

        TaskMessage message = new TaskMessage();
        message.setTaskId(task.getTaskId());
        message.setAgent(task.getAgent());
        message.setAction(task.getAction());
        message.setUserId(context.getUserId());
        message.setUserName(context.getNickname());
        message.setCity(context.getCity());
        message.setSchool(context.getSchool());
        message.setSemesterStart(context.getSemesterStart());
        message.setParams(task.getParameters());
        message.setCorrelationId(correlationId);
        message.setFiles(files);

        pendingResults.put(task.getTaskId(), future);

        messagePublisher.publish(message);

        executor.submit(() -> {
            try {
                ResultMessage result = executeTaskDirectly(task, context, files);
                future.complete(result);
            } catch (Exception e) {
                log.error("任务执行异常: taskId={}", task.getTaskId(), e);
                future.completeExceptionally(e);
            } finally {
                pendingResults.remove(task.getTaskId());
            }
        });

        return future;
    }

    private ResultMessage executeTaskDirectly(Task task, UserContext context, Map<String, String> files) {
        long startTime = System.currentTimeMillis();
        String taskId = task.getTaskId();

        try {
            String correlationId = task.getCorrelationId();
            return switch (task.getAgent().toLowerCase()) {
                case "weather" -> weatherAgent.execute(task.getAction(), context, task.getParameters());
                case "expense" -> expenseAgent.execute(task.getAction(), context, task.getParameters());
                case "course" -> courseAgent.execute(task.getAction(), context, task.getParameters(), files);
                case "profile" -> profileAgent.execute(task.getAction(), context, task.getParameters());
                case "note" -> noteAgent.execute(task.getAction(), context, task.getParameters());
                case "calorie" -> calorieAgent.execute(task.getAction(), context, task.getParameters());
                case "chat" -> chatAgent.execute(task.getAction(), context, task.getParameters());
                case "search" -> searchAgent.execute(task.getAction(), context, task.getParameters());
                default -> ResultMessage.failure(taskId, task.getAgent(), task.getAction(),
                        "未知Agent类型: " + task.getAgent(), correlationId != null ? correlationId : "", context.getUserId());
            };
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}, agent={}", taskId, task.getAgent(), e);
            return ResultMessage.failure(taskId, task.getAgent(), task.getAction(),
                    e.getMessage(), task.getCorrelationId() != null ? task.getCorrelationId() : "", context.getUserId());
        }
    }

    private List<ResultMessage> waitForResults(List<CompletableFuture<ResultMessage>> futures, String correlationId) {
        List<ResultMessage> results = new ArrayList<>();

        for (CompletableFuture<ResultMessage> future : futures) {
            try {
                ResultMessage result = future.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                log.warn("任务超时: correlationId={}", correlationId);
                results.add(ResultMessage.failure("timeout", "unknown", "unknown",
                        "任务执行超时", correlationId, null));
            } catch (Exception e) {
                log.error("获取任务结果失败: correlationId={}", correlationId, e);
                results.add(ResultMessage.failure("error", "unknown", "unknown",
                        e.getMessage(), correlationId, null));
            }
        }

        return results;
    }

    public Map<String, Object> getTaskStatus(String taskId) {
        CompletableFuture<ResultMessage> future = pendingResults.get(taskId);
        Map<String, Object> status = new HashMap<>();
        status.put("taskId", taskId);

        if (future == null) {
            status.put("status", "completed");
        } else if (!future.isDone()) {
            status.put("status", "processing");
        } else if (future.isCompletedExceptionally()) {
            status.put("status", "failed");
        } else {
            status.put("status", "completed");
        }

        return status;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}