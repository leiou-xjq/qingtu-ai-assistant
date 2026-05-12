package com.qingtu.agent.rag;

import com.qingtu.agent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private final RagServiceCore ragServiceCore;
    private final UserMapper userMapper;

    public record RetrievalResult(String query, String esContext, String prompt) {}

    public RetrievalResult retrieve(String query, Long userId) {
        String school = null;
        if (userId != null) {
            var user = userMapper.selectById(userId);
            if (user != null) school = user.getSchool();
        }
        return retrieve(query, school);
    }

    public RetrievalResult retrieve(String query, String school) {
        log.info("RagRetrievalService: query={}, school={}", query, school);

        String esContext = school != null && !school.isBlank()
            ? ragServiceCore.retrieveSchoolContext(school, query, 5)
            : ragServiceCore.retrieveCommonContext(query, 5);

        String prompt = buildPrompt(query, esContext);

        return new RetrievalResult(query, esContext, prompt);
    }

    private String buildPrompt(String query, String context) {
        if (context == null || context.isBlank()) {
            return PromptBuilder.create()
                .system("，专门为大学生提供校园生活服务")
                .question(query)
                .instruction("用中文简洁回答", "禁止使用Markdown格式", "禁止使用引号", "只输出纯文本")
                .build();
        }
        return PromptBuilder.create()
            .system("，专门为大学生提供校园生活服务")
            .context("参考知识", context)
            .question(query)
            .instruction("基于参考信息回答", "如参考信息不足，可补充你的知识", "用中文简洁回答", "禁止使用Markdown格式", "禁止使用引号", "只输出纯文本")
            .build();
    }
}