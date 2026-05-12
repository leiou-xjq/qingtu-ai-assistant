package com.qingtu.agent.service.impl;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingtu.agent.entity.po.SysNotification;
import com.qingtu.agent.exception.BusinessException;
import com.qingtu.agent.common.ResultCode;
import com.qingtu.agent.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 消息通知服务实现类
 * 
 * @author 青途智伴技术团队
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SysNotificationMapper notificationMapper;

    @Override
    public CommonResult<?> listNotifications(Long userId, String type, Integer status, int page, int size) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getDeleted, 0)
                .orderByDesc(SysNotification::getCreateTime);

        if (type != null && !type.isEmpty()) {
            wrapper.eq(SysNotification::getType, type);
        }
        if (status != null) {
            wrapper.eq(SysNotification::getStatus, status);
        }

        Page<SysNotification> pageResult = new Page<>(page, size);
        notificationMapper.selectPage(pageResult, wrapper);

        return CommonResult.success(pageResult);
    }

    @Override
    public CommonResult<?> getUnreadCount(Long userId) {
        long count = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getStatus, 0)
                .eq(SysNotification::getDeleted, 0));
        return CommonResult.success(count);
    }

    @Override
    public CommonResult<?> markAsRead(Long userId, Long notificationId) {
        SysNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        notification.setStatus(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);

        return CommonResult.success("标记成功");
    }

    @Override
    public CommonResult<?> markAllAsRead(Long userId) {
        SysNotification update = new SysNotification();
        update.setStatus(1);
        update.setReadTime(LocalDateTime.now());
        
        notificationMapper.update(update, new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getStatus, 0)
                .eq(SysNotification::getDeleted, 0));

        return CommonResult.success("全部已读");
    }

    @Override
    public CommonResult<?> deleteNotification(Long userId, Long notificationId) {
        SysNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        notificationMapper.deleteById(notificationId);
        return CommonResult.success("删除成功");
    }

    @Override
    public CommonResult<?> sendNotification(Long userId, String type, String title, String content) {
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(0);
        notificationMapper.insert(notification);

        return CommonResult.success("发送成功");
    }
}