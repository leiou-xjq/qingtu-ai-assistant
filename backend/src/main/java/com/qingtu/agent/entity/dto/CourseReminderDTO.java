package com.qingtu.agent.entity.dto;

import lombok.Data;

/**
 * 课程提醒设置 DTO
 */
@Data
public class CourseReminderDTO {
    private boolean enabled;
    private int minutes = 15;
}