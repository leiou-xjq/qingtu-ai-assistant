package com.qingtu.agent.agent.context;

import com.qingtu.agent.entity.po.User;
import com.qingtu.agent.mapper.UserMapper;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户上下文提供者
 * 从 JWT 解析用户信息，供 Orchestrator 和各 Agent 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextProvider {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserContext getContext(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            log.warn("未获取到 Token");
            return null;
        }
        return getContext(token);
    }

    public UserContext getContext(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Long userId = jwtUtil.getUserId(token);
            if (userId == null) {
                log.warn("Token 解析失败，无法获取用户ID");
                return null;
            }

            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return null;
            }

            UserContext context = new UserContext();
            context.setUserId(userId);
            context.setNickname(user.getNickname());
            context.setCity(user.getCity());
            context.setSchool(user.getSchool());
            context.setSemesterStart(user.getSemesterStart() != null
                    ? user.getSemesterStart().toString() : "2025-03-03");
            context.setHeight(user.getHeight());
            context.setWeight(user.getWeight());
            context.setTastePreference(user.getTastePreference());
            context.setToken(token);

            log.debug("获取用户上下文: userId={}, city={}, school={}", userId, context.getCity(), context.getSchool());
            return context;

        } catch (Exception e) {
            log.error("解析用户上下文失败", e);
            return null;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader;
        }
        return null;
    }
}