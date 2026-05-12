package com.qingtu.agent.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serializable;

/**
 * 健康档案DTO
 * 
 * @author 青途智伴技术团队
 */
@Data
public class HealthRecordDTO implements Serializable {

    @NotNull(message = "身高不能为空")
    @Min(value = 100, message = "身高不能小于100cm")
    @Max(value = 250, message = "身高不能大于250cm")
    private Double height;

    @NotNull(message = "体重不能为空")
    @Min(value = 30, message = "体重不能小于30kg")
    @Max(value = 200, message = "体重不能大于200kg")
    private Double weight;

    @Min(value = 10, message = "年龄不能小于10")
    @Max(value = 100, message = "年龄不能大于100")
    private Integer age;

    @NotNull(message = "性别不能为空")
    private String gender;

    @Min(value = 1, message = "活动水平最小1.2")
    @Max(value = 2, message = "活动水平最大1.75")
    private Double activityLevel;

    private String dietGoal;

    private String tabooFood;

    private String tastePreference;
}