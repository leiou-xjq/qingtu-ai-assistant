package com.qingtu.agent.domain.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图分类器（固定规则，已废弃SkillRouter）
 */
@Slf4j
@Component
public class IntentClassifier {

    public IntentType classify(String message) {
        if (message == null || message.isBlank()) return IntentType.CHAT;
        String m = message.toLowerCase();

        // RAG关键词检测
        if (m.contains("大学") || m.contains("校园") || m.contains("学习") || m.contains("考试")
            || m.contains("考研") || m.contains("宿舍") || m.contains("图书馆") || m.contains("食堂")
            || m.contains("课程") || m.contains("专业") || m.contains("就业") || m.contains("留学"))
            return IntentType.RAG;

        return IntentType.CHAT;
    }

    public IntentType classify(Long userId, String message, Map<String, Object> context) {
        return classify(message);
    }
}