package com.qingtu.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingtu.agent.agent.QingTuAgent;
import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.entity.po.*;
import com.qingtu.agent.exception.BusinessException;
import com.qingtu.agent.mapper.ChatMessageMapper;
import com.qingtu.agent.mapper.ChatSessionMapper;
import com.qingtu.agent.mapper.UserHealthMapper;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.rag.RagRetrievalService;
import com.qingtu.agent.rag.RagServiceCore;
import com.qingtu.agent.service.IRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IRagServiceImpl implements IRagService {

    private final RagServiceCore ragServiceCore;
    private final RagRetrievalService ragRetrievalService;
    private final QingTuAgent qingTuAgent;
    private final UserMapper userMapper;
    private final UserHealthMapper userHealthMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public CommonResult<?> ask(Long userId, String question, Long sessionId) {
        RagRetrievalService.RetrievalResult retrieval = ragRetrievalService.retrieve(question, userId);
        String context = retrieval.esContext();
        String userContext = buildUserContext(userId);
        String chatHistory = sessionId != null && sessionId > 0 ? buildChatHistory(sessionId) : "";
        String chatSchool = extractSchoolFromChat(chatHistory);

        StringBuilder prompt = new StringBuilder();

        // 用户档案（优先使用对话中检测到的学校，其次使用档案中的学校）
        String effectiveUserContext;
        if (chatSchool != null && !chatSchool.isEmpty()) {
            effectiveUserContext = buildUserContext(userId, chatSchool);
        } else {
            effectiveUserContext = userContext;
        }
        if (!effectiveUserContext.isEmpty()) {
            prompt.append("用户档案：\n").append(effectiveUserContext).append("\n");
        }

        // 对话历史（最近5轮）
        if (!chatHistory.isEmpty()) {
            prompt.append("对话历史：\n").append(chatHistory).append("\n");
        }

        // 知识库上下文
        prompt.append("参考知识（来自学校官网，请优先使用以下信息回答）：\n").append(context).append("\n");

        // 当前问题
        prompt.append("当前问题：").append(question).append("\n\n");

        // 指令
        prompt.append("请优先使用上方参考知识回答问题，要求：\n")
              .append("1. 如果参考知识中有相关信息，请总结回答并标注来源\n")
              .append("2. 如果参考知识中没有相关信息，直接说明\"抱歉，暂无相关信息\"\n")
              .append("3. 不要重复用户已知的背景信息\n")
              .append("4. 用中文简洁回答\n")
              .append("5. 如果是活动信息，请列出具体时间和地点（如果有）");

        String answer = qingTuAgent.chat(prompt.toString());

        // 自动创建或复用会话
        Long targetSessionId = sessionId;
        if (sessionId == null || sessionId == 0) {
            ChatSession session = createNewSession(userId, question);
            targetSessionId = session.getId();
        } else {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
        }

        // 保存消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(targetSessionId);
        userMsg.setRole("user");
        userMsg.setContent(question);
        chatMessageMapper.insert(userMsg);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(targetSessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(answer);
        chatMessageMapper.insert(assistantMsg);

        // 更新会话时间
        ChatSession updateSession = new ChatSession();
        updateSession.setId(targetSessionId);
        updateSession.setUpdatedAt(java.time.LocalDateTime.now());
        chatSessionMapper.updateById(updateSession);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("context", context);
        result.put("sessionId", targetSessionId);
        return CommonResult.success(result);
    }

    /**
     * 构建对话历史上下文（最近5轮，最多1000字）
     */
    private String buildChatHistory(Long sessionId) {
        try {
            List<ChatMessage> messages = chatMessageMapper.selectList(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getSessionId, sessionId)
                            .orderByDesc(ChatMessage::getCreatedAt)
                            .last("LIMIT 10")  // 最近10条（5轮）
            );

            if (messages.isEmpty()) return "";

            // 反转回时间顺序
            java.util.Collections.reverse(messages);

            StringBuilder sb = new StringBuilder();
            int totalLen = 0;
            for (ChatMessage msg : messages) {
                String line = (msg.getRole().equals("user") ? "用户：" : "助手：") + msg.getContent() + "\n";
                if (totalLen + line.length() > 1000) break;  // 最多1000字
                sb.append(line);
                totalLen += line.length();
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private ChatSession createNewSession(Long userId, String firstQuestion) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(generateSessionTitle(firstQuestion));
        chatSessionMapper.insert(session);
        return session;
    }

    private String generateSessionTitle(String question) {
        String title = question;
        if (title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }
        return title;
    }

    /**
     * 从对话历史中提取最后讨论的学校名
     * 匹配 "大学" 结尾的实体词作为候选学校
     */
    private String extractSchoolFromChat(String chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("([\\u4e00-\\u9fa5]{2,8}(?:大学|学院))")
            .matcher(chatHistory);
        String lastSchool = null;
        while (m.find()) {
            lastSchool = m.group(1);
        }
        return lastSchool;
    }

    private String buildUserContext(Long userId) {
        return buildUserContext(userId, null);
    }

    private String buildUserContext(Long userId, String overrideSchool) {
        StringBuilder sb = new StringBuilder();

        User user = userMapper.selectById(userId);
        if (user != null) {
            String school = overrideSchool != null ? overrideSchool : user.getSchool();
            if (school != null && !school.isEmpty()) {
                sb.append("- 学校：").append(school).append("\n");
            }
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                sb.append("- 城市：").append(user.getCity()).append("\n");
            }
        }

        UserHealth health = userHealthMapper.selectOne(new LambdaQueryWrapper<UserHealth>()
                .eq(UserHealth::getUserId, userId));
        if (health != null) {
            if (health.getAge() != null) {
                sb.append("- 年龄：").append(health.getAge()).append("岁\n");
            }
            if (health.getGender() != null) {
                sb.append("- 性别：").append(health.getGender().equals("M") ? "男" : "女").append("\n");
            }
            if (health.getHeight() != null) {
                sb.append("- 身高：").append(health.getHeight()).append("cm\n");
            }
            if (health.getWeight() != null) {
                sb.append("- 体重：").append(health.getWeight()).append("kg\n");
            }
            if (health.getBmi() != null) {
                sb.append("- BMI：").append(health.getBmi());
                if (health.getBmi().doubleValue() < 18.5) {
                    sb.append("（偏瘦）");
                } else if (health.getBmi().doubleValue() < 24) {
                    sb.append("（正常）");
                } else if (health.getBmi().doubleValue() < 28) {
                    sb.append("（偏胖）");
                } else {
                    sb.append("（肥胖）");
                }
                sb.append("\n");
            }
            if (health.getActivityLevel() != null) {
                String activity;
                double actLevel = health.getActivityLevel().doubleValue();
                if (actLevel == 1.375) {
                    activity = "轻度活跃";
                } else if (actLevel == 1.55) {
                    activity = "中等活跃";
                } else if (actLevel == 1.75) {
                    activity = "高度活跃";
                } else if (actLevel == 1.9) {
                    activity = "重体力";
                } else {
                    activity = "久坐";
                }
                sb.append("- 活动水平：").append(activity).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public CommonResult<?> search(String query, String category, int topK) {
        RagRetrievalService.RetrievalResult retrieval = ragRetrievalService.retrieve(query, (String) null);
        String context = retrieval.esContext();
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("results", context);
        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> getSessionList(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdatedAt)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", session.getId());
            item.put("title", session.getTitle());
            item.put("createdAt", session.getCreatedAt());
            item.put("updatedAt", session.getUpdatedAt());
            result.add(item);
        }
        return CommonResult.success(result);
    }

    @Override
    public CommonResult<?> getSessionHistory(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return CommonResult.success(new ArrayList<>());
        }

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedAt)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", msg.getId());
            item.put("role", msg.getRole());
            item.put("content", msg.getContent());
            item.put("createdAt", msg.getCreatedAt());
            result.add(item);
        }
        return CommonResult.success(result);
    }

    @Override
    @Transactional
    public CommonResult<?> createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        chatSessionMapper.insert(session);

        Map<String, Object> result = new HashMap<>();
        result.put("id", session.getId());
        result.put("title", session.getTitle());
        result.put("createdAt", session.getCreatedAt());
        return CommonResult.success(result);
    }

    @Override
    @Transactional
    public CommonResult<?> deleteSession(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
        chatSessionMapper.deleteById(sessionId);

        return CommonResult.success("删除成功");
    }

    @Override
    public CommonResult<?> renameSession(Long sessionId, String newTitle) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return CommonResult.fail("会话不存在");
        }

        session.setTitle(newTitle);
        session.setUpdatedAt(java.time.LocalDateTime.now());
        chatSessionMapper.updateById(session);

        return CommonResult.success(session);
    }
}
