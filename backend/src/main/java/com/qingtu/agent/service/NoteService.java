package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

/**
 * AI笔记服务接口
 * 
 * 定义AI笔记相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface NoteService {

    /**
     * 获取课程笔记列表
     * 
     * @param userId 用户ID
     * @param courseId 课程ID（可选）
     * @param weekNum 教学周（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 笔记列表
     */
    CommonResult<?> listNotes(Long userId, Long courseId, Integer weekNum, int page, int size);

    /**
     * 获取笔记详情
     * 
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    CommonResult<?> getNoteById(Long userId, Long noteId);

    /**
     * 获取课程的所有笔记
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 笔记列表
     */
    CommonResult<?> getNotesByCourse(Long userId, Long courseId);

    /**
     * 手动生成课程笔记
     * 
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 生成结果
     */
    CommonResult<?> generateNote(Long userId, Long courseId);

    /**
     * 获取每日笔记汇总
     * 
     * @param userId 用户ID
     * @param date 日期（可选，默认今日）
     * @return 汇总内容
     */
    CommonResult<?> getDailySummary(Long userId, String date);

    /**
     * 导出笔记
     * 
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param format 格式（markdown/pdf）
     * @return 导出文件
     */
    CommonResult<?> exportNote(Long userId, Long noteId, String format);

    /**
     * 删除笔记
     * 
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 删除结果
     */
    CommonResult<?> deleteNote(Long userId, Long noteId);
}