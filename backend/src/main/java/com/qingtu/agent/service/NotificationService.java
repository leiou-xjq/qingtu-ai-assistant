package com.qingtu.agent.service;

import com.qingtu.agent.common.CommonResult;

/**
 * 消息通知服务接口
 * 
 * 定义消息相关的业务操作
 * 
 * @author 青途智伴技术团队
 */
public interface NotificationService {

    /**
     * 获取消息列表
     * 
     * @param userId 用户ID
     * @param type 消息类型（可选）
     * @param status 阅读状态（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 消息列表
     */
    CommonResult<?> listNotifications(Long userId, String type, Integer status, int page, int size);

    /**
     * 获取未读消息数量
     * 
     * @param userId 用户ID
     * @return 未读数量
     */
    CommonResult<?> getUnreadCount(Long userId);

    /**
     * 标记消息已读
     * 
     * @param userId 用户ID
     * @param notificationId 消息ID
     * @return 操作结果
     */
    CommonResult<?> markAsRead(Long userId, Long notificationId);

    /**
     * 标记全部已读
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    CommonResult<?> markAllAsRead(Long userId);

    /**
     * 删除消息
     * 
     * @param userId 用户ID
     * @param notificationId 消息ID
     * @return 删除结果
     */
    CommonResult<?> deleteNotification(Long userId, Long notificationId);

    /**
     * 发送站内消息
     * 
     * @param userId 接收用户ID
     * @param type 消息类型
     * @param title 标题
     * @param content 内容
     * @return 发送结果
     */
    CommonResult<?> sendNotification(Long userId, String type, String title, String content);
}