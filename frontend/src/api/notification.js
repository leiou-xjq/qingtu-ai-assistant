/**
 * 消息通知相关API
 */
import request from '../utils/request'

// 获取消息列表
export const getNotificationList = (params) => request.get('/notification/list', params)

// 获取未读数量
export const getUnreadCount = () => request.get('/notification/unread-count')

// 标记已读
export const markAsRead = (id) => request.put(`/notification/${id}/read`)

// 全部已读
export const markAllAsRead = () => request.put('/notification/read-all')

// 删除消息
export const deleteNotification = (id) => request.delete(`/notification/${id}`)