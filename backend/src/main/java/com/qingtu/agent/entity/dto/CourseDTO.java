package com.qingtu.agent.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 课程DTO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class CourseDTO implements Serializable {

    @NotNull(message = "课程名称不能为空")
    private String name;

    private String teacher;

    private String location;

    @NotNull(message = "星期几不能为空")
    private Integer weekday;

    @NotNull(message = "开始时间不能为空")
    private String startTime;

    @NotNull(message = "结束时间不能为空")
    private String endTime;

    private Integer weekStart;

    private Integer weekEnd;

    private String courseType;

    private String color;

    private Integer reminderEnabled;

    private Integer reminderMinutes;

    private String semester;
}