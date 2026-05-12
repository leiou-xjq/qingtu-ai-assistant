package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 课程管理服务接口
 * 
 * 定义课程相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface CourseService {

    /**
     * 获取周课表
     * 
     * @param userId 用户ID
     * @param weekNum 教学周数（可选，默认本周）
     * @return 课表数据
     */
    CommonResult<?> getWeekSchedule(Long userId, Integer weekNum);

    /**
     * 获取今日课程
     * 
     * @param userId 用户ID
     * @return 今日课程列表
     */
    CommonResult<?> getTodayCourses(Long userId);

    /**
     * 生成今日课程AI笔记
     *
     * @param userId 用户ID
     * @return 笔记内容
     */
    CommonResult<?> generateTodayNotes(Long userId);

    /**
     * 添加课程
     * 
     * @param userId 用户ID
     * @param dto 课程信息
     * @return 添加结果
     */
    CommonResult<?> addCourse(Long userId, Object dto);

    /**
     * 更新课程
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param dto 课程信息
     * @return 更新结果
     */
    CommonResult<?> updateCourse(Long userId, Long courseId, Object dto);

    /**
     * 删除课程
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 删除结果
     */
    CommonResult<?> deleteCourse(Long userId, Long courseId);

    /**
     * Excel导入课表
     * 
     * @param userId 用户ID
     * @param file Excel文件
     * @return 导入结果
     */
    CommonResult<?> importSchedule(Long userId, MultipartFile file);

    /**
     * 下载课表导入模板
     * 
     * @return 模板文件
     */
    CommonResult<?> downloadTemplate();

    /**
     * 设置课程提醒
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param enabled 是否启用
     * @param minutes 提前分钟数
     * @return 设置结果
     */
    CommonResult<?> setCourseReminder(Long userId, Long courseId, boolean enabled, int minutes);
}