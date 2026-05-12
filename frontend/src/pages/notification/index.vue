<template>
  <view class="notification-page">
    <!-- 标签筛选 -->
    <view class="filter-tabs">
      <view class="tab" :class="{ active: filterType === '' }" @click="filterType = ''">全部</view>
      <view class="tab" :class="{ active: filterType === 'morning' }" @click="filterType = 'morning'">早安</view>
      <view class="tab" :class="{ active: filterType === 'diet' }" @click="filterType = 'diet'">食谱</view>
      <view class="tab" :class="{ active: filterType === 'course' }" @click="filterType = 'course'">课程</view>
      <view class="tab" :class="{ active: filterType === 'note' }" @click="filterType = 'note'">笔记</view>
      <view class="tab" :class="{ active: filterType === 'cost_report' }" @click="filterType = 'cost_report'">报告</view>
    </view>

    <!-- 消息列表 -->
    <view class="notification-list">
      <view class="notification-item" 
            v-for="item in notifications" 
            :key="item.id"
            :class="{ unread: item.status === 0 }"
            @click="handleRead(item)">
        <view class="item-icon">{{ getTypeIcon(item.type) }}</view>
        <view class="item-content">
          <text class="item-title">{{ item.title }}</text>
          <text class="item-body">{{ item.content }}</text>
          <text class="item-time">{{ formatTime(item.createTime) }}</text>
        </view>
        <view class="unread-dot" v-if="item.status === 0"></view>
      </view>

      <view class="empty-state" v-if="notifications.length === 0">
        <text>暂无消息通知</text>
      </view>
    </view>

    <!-- 全部已读按钮 -->
    <view class="mark-all-btn" v-if="hasUnread" @click="markAllRead">
      <text>全部已读</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getNotificationList, markAsRead, markAllAsRead } from '@/api/notification'

const filterType = ref('')
const notifications = ref([])
const hasUnread = computed(() => notifications.value.some(n => n.status === 0))

const typeIconMap = {
  morning: '☀️',
  diet: '🍽️',
  course: '📚',
  note: '📝',
  cost_report: '💰',
  system: '🔔'
}

const getTypeIcon = (type) => typeIconMap[type] || '🔔'

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString()
}

const loadNotifications = async () => {
  try {
    const res = await getNotificationList({ type: filterType.value, page: 1, size: 50 })
    if (res.data && res.data.records) {
      notifications.value = res.data.records
    }
  } catch (e) {}
}

const handleRead = async (item) => {
  if (item.status === 0) {
    await markAsRead(item.id)
    item.status = 1
  }
  // 跳转到对应页面
  if (item.targetPage) {
    uni.navigateTo({ url: item.targetPage })
  }
}

const markAllRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.status = 1)
    uni.showToast({ title: '已全部已读', icon: 'success' })
  } catch (e) {}
}

onMounted(loadNotifications)
</script>

<style scoped>
.notification-page {
  min-height: 100vh;
  background: #F8FAFC;
}

.filter-tabs {
  display: flex;
  background: #fff;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  padding: 12rpx 24rpx;
  font-size: 28rpx;
  color: #666;
  border-radius: 32rpx;
  margin-right: 16rpx;
}

.tab.active {
  background: #3B82F6;
  color: #fff;
}

.notification-list {
  padding: 24rpx;
}

.notification-item {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  position: relative;
}

.notification-item.unread {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08) 0%, rgba(255, 255, 255, 1) 100%);
}

.item-icon {
  width: 80rpx;
  height: 80rpx;
  background: #f0f5ff;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.item-content {
  flex: 1;
}

.item-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.item-body {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 12rpx;
  lines: 2;
  text-overflow: ellipsis;
}

.item-time {
  font-size: 22rpx;
  color: #999;
}

.unread-dot {
  width: 16rpx;
  height: 16rpx;
  background: #FF6B6B;
  border-radius: 50%;
  position: absolute;
  top: 28rpx;
  right: 28rpx;
}

.mark-all-btn {
  position: fixed;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border: 2rpx solid #3B82F6;
  color: #3B82F6;
  padding: 20rpx 48rpx;
  border-radius: 48rpx;
  font-size: 28rpx;
}

.empty-state {
  text-align: center;
  padding: 100rpx;
  color: #999;
  font-size: 28rpx;
}
</style>