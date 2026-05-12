package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.entity.dto.*;

/**
 * 健康档案服务接口
 * 
 * 定义健康档案相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface HealthService {

    /**
     * 获取健康档案
     * 
     * @param userId 用户ID
     * @return 健康档案信息
     */
    CommonResult<?> getHealthRecord(Long userId);

    /**
     * 创建/更新健康档案
     * 
     * @param userId 用户ID
     * @param dto 健康信息
     * @return 保存结果
     */
    CommonResult<?> saveHealthRecord(Long userId, HealthRecordDTO dto);

    /**
     * 计算BMI
     * 
     * @param height 身高（厘米）
     * @param weight 体重（公斤）
     * @return BMI结果
     */
    CommonResult<?> calculateBmi(Double height, Double weight);

    /**
     * 获取健康建议
     * 
     * @param userId 用户ID
     * @return AI生成的健康建议
     */
    CommonResult<?> getHealthSuggestion(Long userId);
}