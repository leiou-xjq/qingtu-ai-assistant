package com.qingtu.agent.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PromptSecurityFilter {

    private static final int MAX_INPUT_LENGTH = 2000;

    private static final Pattern ROLE_PLAY_PATTERN = Pattern.compile(
            "(?i)(system|ignore|you are|act as|roleplay)"
    );

    private static final Pattern INJECTION_PATTERNS[] = {
            Pattern.compile("(?i)(ignore previous instructions)"),
            Pattern.compile("(?i)(disregard all previous)"),
            Pattern.compile("(?i)(forget everything)"),
            Pattern.compile("(?i)(reveal your system)"),
            Pattern.compile("(?i)(bypass| jailbreak)"),
            Pattern.compile("(.{20,})\\1{5,}")  // 重复字符检测
    };

    public SecurityCheckResult check(String input) {
        if (input == null || input.isBlank()) {
            return new SecurityCheckResult(false, "输入不能为空");
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            return new SecurityCheckResult(false, "输入长度不能超过" + MAX_INPUT_LENGTH + "字符");
        }

        if (ROLE_PLAY_PATTERN.matcher(input).find()) {
            log.warn("检测到角色扮演指令: {}", input.substring(0, Math.min(50, input.length())));
            return new SecurityCheckResult(false, "检测到潜在的安全风险");
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到注入攻击: {}", input.substring(0, Math.min(50, input.length())));
                return new SecurityCheckResult(false, "输入包含敏感内容");
            }
        }

        if (containsSuspiciousEncoding(input)) {
            return new SecurityCheckResult(false, "检测到可疑编码");
        }

        return new SecurityCheckResult(true, null);
    }

    private boolean containsSuspiciousEncoding(String input) {
        return input.toLowerCase().contains("base64") ||
               input.toLowerCase().contains("urlencode");
    }

    public record SecurityCheckResult(boolean passed, String reason) {}
}