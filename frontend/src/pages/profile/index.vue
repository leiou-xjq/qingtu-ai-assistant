<template>
  <view class="profile-page">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="user-avatar">
        <image :src="userInfo.avatar || '/static/default-avatar.png'" mode="aspectFill" />
      </view>
      <view class="user-info">
        <text class="username">{{ userInfo.nickname || userInfo.username }}</text>
        <text class="user-desc">大学生用户</text>
      </view>
      <view class="edit-btn" @click="goEditProfile">编辑</view>
    </view>

    <!-- 健康档案 -->
    <view class="menu-card">
      <view class="menu-item" @click="goPage('/pages/diet/index')">
        <view class="menu-left">
          <text class="menu-icon">❤️</text>
          <text class="menu-text">健康档案</text>
        </view>
        <view class="menu-right">
          <text class="menu-value" v-if="healthInfo.bmi">BMI {{ healthInfo.bmi }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card">
      <view class="menu-item" @click="goPage('/pages/notification/index')">
        <view class="menu-left">
          <text class="menu-icon">🔔</text>
          <text class="menu-text">消息通知</text>
        </view>
        <view class="menu-right">
          <text class="arrow">›</text>
        </view>
      </view>
      
      <view class="menu-item" @click="goPage('/pages/note/index')">
        <view class="menu-left">
          <text class="menu-icon">📝</text>
          <text class="menu-text">我的笔记</text>
        </view>
        <view class="menu-right">
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="handleLogout">退出登录</view>

    <view class="version">v1.0.0</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserInfo, logout } from '@/api/user'
import { getHealthKpi } from '@/api/health'

const userInfo = ref({})
const healthInfo = ref({})

const goPage = (path) => {
  uni.navigateTo({ url: path })
}

const goEditProfile = () => {
  uni.navigateTo({ url: '/pages/profile/edit' })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await logout()
        } catch (e) {}
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    }
  })
}

const loadData = async () => {
  try {
    const res = await getUserInfo()
    if (res.data) userInfo.value = res.data
  } catch (e) {
    console.log('获取用户信息失败')
  }

  try {
    const healthRes = await getHealthKpi()
    if (healthRes.data) healthInfo.value = healthRes.data
  } catch (e) {
    console.log('获取健康档案失败')
  }
}

onShow(() => {
  loadData()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx;
}

.user-card {
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 24rpx;
  padding: 40rpx;
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: #fff;
  overflow: hidden;
  margin-right: 24rpx;
}

.user-avatar image {
  width: 100%;
  height: 100%;
}

.user-info {
  flex: 1;
  color: #fff;
}

.username {
  font-size: 36rpx;
  font-weight: 600;
  display: block;
  margin-bottom: 8rpx;
}

.user-desc {
  font-size: 26rpx;
  opacity: 0.85;
}

.edit-btn {
  background: rgba(255,255,255,0.2);
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  font-size: 26rpx;
  color: #fff;
}

.menu-card {
  background: #fff;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.menu-text {
  font-size: 30rpx;
  color: #333;
}

.menu-right {
  display: flex;
  align-items: center;
}

.menu-value {
  font-size: 26rpx;
  color: #999;
  margin-right: 12rpx;
}

.arrow {
  font-size: 36rpx;
  color: #ccc;
}

.logout-btn {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  text-align: center;
  color: #FF6B6B;
  font-size: 30rpx;
  margin-bottom: 24rpx;
}

.version {
  text-align: center;
  font-size: 24rpx;
  color: #ccc;
}
</style>