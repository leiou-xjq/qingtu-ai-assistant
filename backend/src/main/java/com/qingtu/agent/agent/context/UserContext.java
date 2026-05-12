package com.qingtu.agent.agent.context;

import lombok.Data;

/**
 * 用户上下文
 * 从 JWT 解析，用于在各 Agent 间传递
 */
@Data
public class UserContext {

    private Long userId;
    private String nickname;
    private String city;
    private String school;
    private String semesterStart;
    private Double height;
    private Double weight;
    private String tastePreference;
    private String token;

    public static UserContext of(Long userId, String nickname, String city, String school) {
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setNickname(nickname);
        context.setCity(city);
        context.setSchool(school);
        return context;
    }

    public void setDefaultIfAbsent(UserContext other) {
        if (this.city == null || this.city.isBlank()) {
            this.city = other.getCity();
        }
        if (this.school == null || this.school.isBlank()) {
            this.school = other.getSchool();
        }
        if (this.semesterStart == null || this.semesterStart.isBlank()) {
            this.semesterStart = other.getSemesterStart();
        }
    }
}