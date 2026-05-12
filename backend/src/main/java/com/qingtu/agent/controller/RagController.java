package com.qingtu.agent.controller;

import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.agent.orchestrator.OrchestratorAgent;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.infrastructure.redis.RedisSessionManager;
import com.qingtu.agent.infrastructure.redis.RedisSessionManager.ChatMessage;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.mapper.ChatMessageMapper;
import com.qingtu.agent.mapper.ChatSessionMapper;
import com.qingtu.agent.rag.ElasticsearchRagStore;
import com.qingtu.agent.rag.RagRetrievalService;
import com.qingtu.agent.service.DocumentParserService;
import com.qingtu.agent.service.IRagService;
import com.qingtu.agent.service.AsyncRagService;
import com.qingtu.agent.util.AliyunOssUtil;
import com.qingtu.agent.util.JwtUtil;
import com.qingtu.agent.entity.dto.AskDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final IRagService ragService;
    private final AsyncRagService asyncRagService;
    private final ElasticsearchRagStore esRagStore;
    private final JwtUtil jwtUtil;
    private final RagRetrievalService ragRetrievalService;
    private final QingTuAgent qingTuAgent;
    private final UserMapper userMapper;
    private final RedisSessionManager redisSessionManager;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final OrchestratorAgent orchestratorAgent;
    private final DocumentParserService documentParserService;
    private final AliyunOssUtil aliyunOssUtil;

    // ========== API 端点 ==========

    @PostMapping("/ask")
    public CommonResult<?> ask(HttpServletRequest request, @RequestBody AskDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return ragService.ask(userId, dto.getQuestion(), dto.getSessionId());
    }

    @GetMapping("/sessions")
    public CommonResult<?> getSessionList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ragService.getSessionList(userId);
    }

    @GetMapping("/sessions/{sessionId}/history")
    public CommonResult<?> getSessionHistory(@PathVariable Long sessionId) {
        return ragService.getSessionHistory(sessionId);
    }

    @PostMapping("/sessions")
    public CommonResult<?> createSession(HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
        Long userId = getUserIdFromRequest(request);
        String title = body != null ? body.get("title") : null;
        return ragService.createSession(userId, title);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public CommonResult<?> deleteSession(@PathVariable Long sessionId) {
        return ragService.deleteSession(sessionId);
    }

    @PutMapping("/sessions/{sessionId}")
    public CommonResult<?> renameSession(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        return ragService.renameSession(sessionId, title);
    }

    @GetMapping("/search")
    public CommonResult<?> search(@RequestParam String query,
                                @RequestParam(required = false) String category,
                                @RequestParam(defaultValue = "5") int topK) {
        return ragService.search(query, category, topK);
    }

    @PostMapping("/ask-async")
    public CommonResult<?> askAsync(HttpServletRequest request, @RequestBody AskDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return asyncRagService.createTask(userId, dto.getQuestion(), dto.getSessionId());
    }

    @PostMapping("/ask-skill")
    public CommonResult<?> askWithSkill(HttpServletRequest request, @RequestBody AskDTO dto) {
        Long userId = getUserIdFromRequest(request);
        return asyncRagService.createSkillTask(userId, dto.getQuestion(), dto.getSessionId());
    }

    @GetMapping("/task/{taskId}/status")
    public CommonResult<?> getTaskStatus(@PathVariable Long taskId) {
        return asyncRagService.getTaskStatus(taskId);
    }

    @GetMapping("/task/{taskId}/result")
    public CommonResult<?> getTaskResult(@PathVariable Long taskId) {
        return asyncRagService.getTaskResult(taskId);
    }

    @DeleteMapping("/clear")
    public CommonResult<?> clearKnowledgeBase() {
        esRagStore.recreateIndex();
        return CommonResult.success("知识库已清空并重建");
    }

    @PostMapping("/upload")
    public CommonResult<?> uploadFile(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long userId = getUserIdFromRequest(request);
        log.info("收到文件上传请求: fileName={}, userId={}", file.getOriginalFilename(), userId);

        try {
            String url = aliyunOssUtil.uploadFile(file);
            if (url != null) {
                return CommonResult.success(Map.of("url", url, "fileName", file.getOriginalFilename()));
            }
            return CommonResult.fail("文件上传失败");
        } catch (Exception e) {
            log.error("文件上传异常", e);
            return CommonResult.fail("文件上传失败：" + e.getMessage());
        }
    }

    // ========== 核心：统一问答流式接口（完全依赖LLM意图识别）==========

    @GetMapping(value = "/ask-stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> askStream(HttpServletRequest request,
                                   @RequestParam String question,
                                   @RequestParam(required = false) Long sessionId) {
        Long userId = getUserIdFromRequest(request);
        log.info("收到问答请求: question={}, userId={}, sessionId={}", question, userId, sessionId);
        return handleAgentTask(userId, sessionId, question, null);
    }

    @PostMapping(value = "/ask-stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> askStreamWithFile(HttpServletRequest request,
                                           @RequestParam String question,
                                           @RequestParam(required = false) Long sessionId,
                                           @RequestParam(required = false) MultipartFile file) {
        Long userId = getUserIdFromRequest(request);
        log.info("收到问答请求(带文件): question={}, userId={}, file={}", question, userId, file != null ? file.getOriginalFilename() : "none");
        return handleAgentTaskWithFile(userId, sessionId, question, file);
    }

    @PostMapping(value = "/ask-with-file", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> askWithFileUpload(HttpServletRequest request,
                                          @RequestParam String question,
                                          @RequestParam(required = false) Long sessionId,
                                          @RequestParam(required = false) MultipartFile file) {
        Long userId = getUserIdFromRequest(request);
        log.info("收到文件上传问答请求: question={}, userId={}, file={}", question, userId, file != null ? file.getOriginalFilename() : "none");
        return handleAgentTaskWithFile(userId, sessionId, question, file);
    }

    @PostMapping(value = "/ask-with-file-url", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> askWithFileUrl(HttpServletRequest request,
                                        @RequestParam String question,
                                        @RequestParam(required = false) Long sessionId,
                                        @RequestParam(required = false) String fileUrl) {
        Long userId = getUserIdFromRequest(request);
        log.info("收到文件URL问答请求: question={}, userId={}, fileUrl={}", question, userId, fileUrl);
        return handleAgentTaskWithFileUrl(userId, sessionId, question, fileUrl);
    }

    // ========== 对话持久化 ==========

    /** 保存用户提问和AI回复到Redis和MySQL */
    private void saveConversation(Long userId, Long sessionId, String question, String answer) {
        if (sessionId == null || userId == null) return;
        try {
            // Redis（实时对话上下文，30分钟TTL）
            redisSessionManager.appendMessage(userId, sessionId.toString(),
                    RedisSessionManager.MessageRole.USER, question);
            redisSessionManager.appendMessage(userId, sessionId.toString(),
                    RedisSessionManager.MessageRole.ASSISTANT, answer);
        } catch (Exception e) {
            // Redis不可用时不影响主流程
        }
        try {
            // MySQL（持久化存储，跨会话恢复）
            com.qingtu.agent.entity.po.ChatMessage userMsg = new com.qingtu.agent.entity.po.ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(question);
            chatMessageMapper.insert(userMsg);

            com.qingtu.agent.entity.po.ChatMessage assistantMsg = new com.qingtu.agent.entity.po.ChatMessage();
            assistantMsg.setSessionId(sessionId);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(answer);
            chatMessageMapper.insert(assistantMsg);

            com.qingtu.agent.entity.po.ChatSession session = new com.qingtu.agent.entity.po.ChatSession();
            session.setId(sessionId);
            session.setUpdatedAt(java.time.LocalDateTime.now());
            chatSessionMapper.updateById(session);
        } catch (Exception e) {
            // MySQL不可用时不影响主流程
        }
    }

    // ========== 对话处理（greeting/talk） ==========

    private Flux<String> handleConversation(Long userId, Long sessionId, String question, String intent) {
        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> {
            User user = userId != null ? userMapper.selectById(userId) : null;
            List<ChatMessage> history = getHistory(userId, sessionId);
            return buildConversationPrompt(intent, question, user, history);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(qingTuAgent::chatStream)
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> saveConversation(userId, sessionId, question, fullAnswer.toString()))
        .onErrorResume(e -> Flux.just("抱歉，我暂时无法回应，请稍后再试~"))
        ;
    }

    private List<ChatMessage> getHistory(Long userId, Long sessionId) {
        try {
            if (sessionId != null && userId != null) {
                List<ChatMessage> h = redisSessionManager.getHistory(userId, sessionId.toString());
                return h.size() > 10 ? h.subList(h.size() - 10, h.size()) : h;
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }

    private String buildConversationPrompt(String intent, String question, User user, List<ChatMessage> history) {
        StringBuilder p = new StringBuilder();
        p.append("你是青途智伴，专为大学生打造的AI生活助手。你温暖、善解人意、充满正能量。\n\n");

        if (user != null) {
            p.append("正在与你对话的用户：").append(user.getNickname() != null ? user.getNickname() : user.getUsername());
            if (user.getSchool() != null) p.append("，").append(user.getSchool()).append("的学生");
            if (user.getCity() != null) p.append("，生活在").append(user.getCity());
            p.append("。\n\n");
        }

        if (!history.isEmpty()) {
            p.append("对话历史：\n");
            for (ChatMessage m : history) p.append(m.role()).append(": ").append(m.content()).append("\n");
            p.append("\n");
        }

        if ("greeting".equals(intent)) {
            p.append("用户正在问候或询问你的身份，请用亲切友好的语气回复，控制在100字以内。\n");
        } else {
            p.append("用户正在谈心交流，请用温暖真诚的语气回复，表达共情和理解，控制在150字以内。\n");
        }

        p.append("\n用户说：").append(question);
        p.append("\n\n请直接回复。不要使用任何引号或Markdown格式，只输出你说的话。");
        return p.toString();
    }

    // ========== RAG 流水线 ==========

    private Flux<String> handleRagPipeline(Long userId, Long sessionId, String question) {
        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> ragRetrievalService.retrieve(question, userId))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(result -> {
                if (result.prompt() == null || result.prompt().isBlank()) {
                    return Flux.just("抱歉，我暂时无法回答这个问题，请换个问题试试~");
                }
                return qingTuAgent.chatStream(result.prompt());
            })
            .doOnNext(fullAnswer::append)
            .doOnComplete(() -> saveConversation(userId, sessionId, question, fullAnswer.toString()))
            .onErrorResume(e -> Flux.just("抱歉，服务暂时不可用，请稍后重试~"))
            ;
    }

    // ========== 业务处理（直接调用已有Service） ==========

    private Flux<String> handleWeather(Long userId, String question) {
        User user = userId != null ? userMapper.selectById(userId) : null;
        String city = (user != null && user.getCity() != null) ? user.getCity() : "北京";
        StringBuilder fullAnswer = new StringBuilder();

        return Mono.fromCallable(() -> {
            com.qingtu.agent.util.WeatherUtil weatherUtil =
                new com.qingtu.agent.util.WeatherUtil(new com.qingtu.agent.config.WeatherConfig());
            com.qingtu.agent.util.WeatherUtil.WeatherInfo info = weatherUtil.getCurrentWeather(city);
            return info.getWeatherSummary();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(data -> {
            String prompt = com.qingtu.agent.rag.PromptBuilder.create()
                .system("你是青途智伴AI助手")
                .context("天气数据", data)
                .question(question).build();
            return qingTuAgent.chatStream(prompt);
        })
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> saveConversation(userId, null, question, fullAnswer.toString()))
        .onErrorResume(e -> Flux.just(buildWeatherText(city)))
        ;
    }

    private String buildWeatherText(String city) {
        return city + "天气信息暂时获取失败，请稍后重试~";
    }

    private Flux<String> handleCourse(Long userId, String question) {
        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> "课程查询功能请使用课表页面查看详细信息。")
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(msg -> qingTuAgent.chatStream(
                com.qingtu.agent.rag.PromptBuilder.create()
                    .system("你是青途智伴AI助手").question(question).build()))
            .doOnNext(fullAnswer::append)
            .doOnComplete(() -> saveConversation(userId, null, question, fullAnswer.toString()))
            .onErrorResume(e -> Flux.just("课程数据查询失败~"))
            ;
    }

    private Flux<String> handleCost(Long userId, String question) {
        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> "记账功能请使用消费记账页面记录和查看。")
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(msg -> qingTuAgent.chatStream(
                com.qingtu.agent.rag.PromptBuilder.create()
                    .system("你是青途智伴AI助手").question(question).build()))
            .doOnNext(fullAnswer::append)
            .doOnComplete(() -> saveConversation(userId, null, question, fullAnswer.toString()))
            .onErrorResume(e -> Flux.just("消费数据查询失败~"))
            ;
    }

    private Flux<String> handleDiet(Long userId, String question) {
        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> "饮食推荐功能正在准备中，请使用饮食页面查看食堂菜品。")
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(msg -> qingTuAgent.chatStream(
                com.qingtu.agent.rag.PromptBuilder.create()
                    .system("你是青途智伴AI助手").question(question).build()))
            .doOnNext(fullAnswer::append)
            .doOnComplete(() -> saveConversation(userId, null, question, fullAnswer.toString()))
            .onErrorResume(e -> Flux.just("饮食数据查询失败~"))
            ;
    }

    private Flux<String> handleOutfit(Long userId, String question) {
        User user = userId != null ? userMapper.selectById(userId) : null;
        String city = (user != null && user.getCity() != null) ? user.getCity() : "北京";
        StringBuilder fullAnswer = new StringBuilder();

        return Mono.fromCallable(() -> {
            com.qingtu.agent.util.WeatherUtil weatherUtil =
                new com.qingtu.agent.util.WeatherUtil(new com.qingtu.agent.config.WeatherConfig());
            com.qingtu.agent.util.WeatherUtil.WeatherInfo info = weatherUtil.getCurrentWeather(city);
            return city + "当前" + info.getTemp() + "C，" + info.getText() + "。请根据天气和场景推荐今日穿搭。";
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(data -> qingTuAgent.chatStream(
            com.qingtu.agent.rag.PromptBuilder.create()
                .system("你是青途智伴AI助手，专门为大学生提供生活建议")
                .context("天气", data)
                .question(question)
                .instruction("根据天气情况推荐合适的穿搭", "考虑大学生的日常场景", "控制在100字以内")
                .build()))
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> saveConversation(userId, null, question, fullAnswer.toString()))
        .onErrorResume(e -> Flux.just("穿搭建议暂时不可用~"))
        ;
    }

    private Flux<String> handleNote(Long userId, String question) {
        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> "笔记功能请使用笔记页面查看和生成课程笔记。")
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(msg -> qingTuAgent.chatStream(
                com.qingtu.agent.rag.PromptBuilder.create()
                    .system("你是青途智伴AI助手").question(question).build()))
            .doOnNext(fullAnswer::append)
            .doOnComplete(() -> saveConversation(userId, null, question, fullAnswer.toString()))
            .onErrorResume(e -> Flux.just("笔记功能暂时不可用~"))
            ;
    }

    // ========== 多Agent任务处理 ==========

    private Flux<String> handleAgentTask(Long userId, Long sessionId, String question, String agentType) {
        log.info("多Agent任务: question={}, userId={}", question, userId);

        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> {
            try {
                User user = userMapper.selectById(userId);
                String username = user != null ? user.getUsername() : "user";
                String token = jwtUtil.generateToken(userId, username);
                String result = orchestratorAgent.process(question, "Bearer " + token, new HashMap<>(),
                    sessionId != null ? String.valueOf(sessionId) : "");
                log.info("Orchestrator返回: {}", result);
                return result != null ? result : "处理完成";
            } catch (Exception e) {
                log.error("多Agent任务失败", e);
                return "处理失败：" + e.getMessage();
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(result -> {
            String prompt = com.qingtu.agent.rag.PromptBuilder.create()
                .system("你是青途智伴AI助手，简洁回复用户")
                .context("处理结果", result)
                .question(question)
                .instruction("直接告诉用户处理结果，不需要额外解释", "如果失败，告知用户稍后重试")
                .build();
            return qingTuAgent.chatStream(prompt);
        })
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> saveConversation(userId, sessionId, question, fullAnswer.toString()))
        .onErrorResume(e -> {
            log.error("多Agent任务异常", e);
            return Flux.just("抱歉，服务暂时不可用，请稍后重试~");
        })
        ;
    }

    private Flux<String> handleAgentTaskWithFile(Long userId, Long sessionId, String question, MultipartFile file) {
        log.info("多Agent任务(带文件): question={}, userId={}, file={}", question, userId, file != null ? file.getOriginalFilename() : "none");

        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> {
            String documentContent = "";
            if (file != null && !file.isEmpty()) {
                String filename = file.getOriginalFilename();
                try {
                    List<String> chunks = documentParserService.parseDocument(file);
                    documentContent = String.join("\n\n", chunks);
                    log.info("文件解析成功: {}, {} chars", filename, documentContent.length());
                } catch (Exception e) {
                    log.warn("文件解析失败: {}", e.getMessage());
                    documentContent = "[文件内容解析失败]";
                }
            }
            return documentContent;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(documentContent -> {
            String prompt = com.qingtu.agent.rag.PromptBuilder.create()
                .system("你是青途智伴AI助手，基于用户提供的文档内容回答问题。回答要准确、简洁、有条理。")
                .context("文档内容", documentContent)
                .question(question)
                .instruction("根据文档内容回答用户问题", "如果文档中没有相关信息，请如实告知", "回答简洁明了，突出重点")
                .build();
            return qingTuAgent.chatStream(prompt);
        })
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> saveConversation(userId, sessionId, question, fullAnswer.toString()))
        .onErrorResume(e -> {
            log.error("文件问答异常", e);
            return Flux.just("抱歉，文件处理失败，请稍后重试~");
        })
        ;
    }

    private Flux<String> handleAgentTaskWithFileUrl(Long userId, Long sessionId, String question, String fileUrl) {
        log.info("多Agent任务(带文件URL): question={}, userId={}, fileUrl={}", question, userId, fileUrl);

        StringBuilder fullAnswer = new StringBuilder();
        return Mono.fromCallable(() -> {
            String documentContent = "";
            if (fileUrl != null && !fileUrl.isBlank()) {
                try {
                    List<String> chunks = documentParserService.parseDocument(fileUrl, null);
                    documentContent = String.join("\n\n", chunks);
                    log.info("文件URL解析成功: {}, {} chars", fileUrl, documentContent.length());
                } catch (Exception e) {
                    log.warn("文件URL解析失败: {}", e.getMessage());
                    documentContent = "[文件内容解析失败]";
                }
            }
            return documentContent;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(documentContent -> {
            String prompt = com.qingtu.agent.rag.PromptBuilder.create()
                .system("你是青途智伴AI助手，基于用户提供的文档内容回答问题。回答要准确、简洁、有条理。")
                .context("文档内容", documentContent)
                .question(question)
                .instruction("根据文档内容回答用户问题", "如果文档中没有相关信息，请如实告知", "回答简洁明了，突出重点")
                .build();
            return qingTuAgent.chatStream(prompt);
        })
        .doOnNext(fullAnswer::append)
        .doOnComplete(() -> saveConversation(userId, sessionId, question, fullAnswer.toString()))
        .onErrorResume(e -> {
            log.error("文件URL问答异常", e);
            return Flux.just("抱歉，文件处理失败，请稍后重试~");
        })
        ;
    }

    // ========== 工具方法 ==========

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}