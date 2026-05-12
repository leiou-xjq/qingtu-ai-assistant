package com.qingtu.agent.controller;

import com.qingtu.agent.common.CommonResult;
import com.qingtu.agent.service.NotificationService;
import com.qingtu.agent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消息通知控制器
 * 
 * 提供消息列表、已读管理等功能
 * 
 * @author 青途智伴技术团队
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    /**
     * 获取消息列表
     */
    @GetMapping("/list")
    public CommonResult<?> listNotifications(HttpServletRequest request,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserIdFromRequest(request);
        return notificationService.listNotifications(userId, type, status, page, size);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public CommonResult<?> getUnreadCount(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return notificationService.getUnreadCount(userId);
    }

    /**
     * 标记消息已读
     */
    @PutMapping("/{id}/read")
    public CommonResult<?> markAsRead(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return notificationService.markAsRead(userId, id);
    }

    /**
     * 标记全部已读
     */
    @PutMapping("/read-all")
    public CommonResult<?> markAllAsRead(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return notificationService.markAllAsRead(userId);
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{id}")
    public CommonResult<?> deleteNotification(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return notificationService.deleteNotification(userId, id);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        return jwtUtil.getUserId(token);
    }
}