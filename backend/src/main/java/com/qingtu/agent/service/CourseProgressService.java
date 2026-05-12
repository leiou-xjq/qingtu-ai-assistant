package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

/**
 * 课程进度服务
 */
public interface CourseProgressService {

    /**
     * 分析课程进度
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 课程进度分析结果
     */
    CommonResult<?> analyzeCourseProgress(Long userId, Long courseId);

    /**
     * 生成指定课程的笔记
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param weekNum 周数
     * @return 生成的笔记
     */
    CommonResult<?> generateCourseNote(Long userId, Long courseId, Integer weekNum);
}
