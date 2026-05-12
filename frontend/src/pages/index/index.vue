<template>
  <view class="index-page">
    <!-- 顶部天气卡片 -->
    <view class="weather-card" @click="goPage('/pages/weather/index')">
      <view class="weather-info">
        <text class="temp">{{ weatherInfo.temp }}°</text>
        <view class="weather-detail">
          <text class="city">{{ weatherInfo.cityName }}</text>
          <text class="text">{{ weatherInfo.text }}</text>
        </view>
      </view>
      <view class="outfit-tip">
        <text class="tip-text">{{ weatherInfo.outfitTip }}</text>
      </view>
    </view>

    <!-- 今日课程 -->
    <view class="section-card" @click="goPage('/pages/course/index')">
      <view class="section-header">
        <text class="section-title">今日课程</text>
        <text class="section-more">查看全部</text>
      </view>
      <view class="course-list" v-if="todayCourses.length > 0">
        <view class="course-item" v-for="course in todayCourses" :key="course.id">
          <view class="course-time">
            <text>{{ course.startTime }}</text>
          </view>
          <view class="course-info">
            <text class="course-name">{{ course.name }}</text>
            <text class="course-location">{{ course.location }}</text>
          </view>
        </view>
      </view>
      <view class="empty-state" v-else>
        <text>今日暂无课程安排~</text>
      </view>
    </view>

    <!-- 消费概览 -->
    <view class="section-card" @click="goPage('/pages/cost/index')">
      <view class="section-header">
        <text class="section-title">本月消费</text>
        <text class="section-more">查看详情</text>
      </view>
      <view class="cost-overview">
        <text class="cost-amount">¥{{ costStats.totalAmount || '0.00' }}</text>
        <view class="cost-chart">
          <view v-for="item in costStats.categoryStats" :key="item.category" class="chart-item">
            <view class="chart-bar" :style="{ width: item.percentage + '%' }"></view>
            <text class="chart-label">{{ item.category }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 快捷功能 -->
    <view class="quick-actions">
      <view class="action-item" v-for="action in quickActions" :key="action.path" @click="goPage(action.path)">
        <view class="action-icon" :style="{ backgroundColor: action.bgColor }">
          <text class="iconfont">{{ action.icon }}</text>
        </view>
        <text class="action-name">{{ action.name }}</text>
      </view>
    </view>

    <!-- AI问答入口 -->
    <view class="ai-card" @click="goPage('/pages/chat/index')">
      <view class="ai-content">
        <text class="ai-title">AI智能助手</text>
        <text class="ai-desc">校园知识、学习生活，我来帮你解答</text>
      </view>
      <view class="ai-arrow">›</view>
    </view>

    <!-- 消息通知 -->
    <view class="notification-bar" v-if="unreadCount > 0" @click="goPage('/pages/notification/index')">
      <view class="notification-badge"></view>
      <text class="notification-text">您有{{ unreadCount }}条未读消息</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getCurrentWeather, preloadOutfitSuggestion } from '@/api/weather'
import { getTodayCourses } from '@/api/course'
import { getMonthlyStatistics } from '@/api/cost'
import { getUnreadCount } from '@/api/notification'

const weatherInfo = ref({
  cityName: '北京',
  temp: '25',
  text: '多云',
  outfitTip: '今日适合穿薄长袖'
})

const todayCourses = ref([])
const costStats = ref({})
const unreadCount = ref(0)

const quickActions = [
  { name: '健康饮食', icon: '🍽️', path: '/pages/diet/index', bgColor: '#E8F5E9' },
  { name: 'AI笔记', icon: '📝', path: '/pages/note/index', bgColor: '#E3F2FD' },
  { name: '记账', icon: '💰', path: '/pages/cost/index', bgColor: '#FFF3E0' },
  { name: '课程', icon: '📚', path: '/pages/course/index', bgColor: '#F3E5F5' }
]

const goPage = (path) => {
  const tabBarPages = ['/pages/index/index', '/pages/cost/index', '/pages/course/index', '/pages/chat/index', '/pages/profile/index']
  if (tabBarPages.includes(path)) {
    uni.switchTab({ url: path })
  } else {
    uni.navigateTo({ url: path })
  }
}

const loadWeather = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  const city = userInfo?.city || '北京'
  weatherInfo.value.cityName = city
  
  try {
    const weatherRes = await getCurrentWeather(city)
    if (weatherRes.data) weatherInfo.value = weatherRes.data
  } catch (e) {
    console.log('天气加载失败')
  }
}

const loadData = async () => {
  try {
    const courseRes = await getTodayCourses()
    if (courseRes.data) todayCourses.value = courseRes.data.slice(0, 3)
  } catch (e) {
    console.log('课程加载失败')
  }

  try {
    const now = new Date()
    const costRes = await getMonthlyStatistics(now.getFullYear(), now.getMonth() + 1)
    if (costRes.data) costStats.value = costRes.data
  } catch (e) {
    console.log('消费统计加载失败')
  }

  try {
    const notiRes = await getUnreadCount()
    if (notiRes.data) unreadCount.value = notiRes.data
  } catch (e) {
    console.log('通知加载失败')
  }
}

onShow(() => {
  preloadOutfitSuggestion()  // 预加载穿搭建议到缓存
  loadWeather()
  loadData()
})
</script>

<style scoped>
.index-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx;
}

.weather-card {
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 24rpx;
  padding: 32rpx;
  color: #fff;
  margin-bottom: 24rpx;
}

.weather-info {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.temp {
  font-size: 72rpx;
  font-weight: 300;
  margin-right: 24rpx;
}

.weather-detail {
  display: flex;
  flex-direction: column;
}

.city {
  font-size: 28rpx;
  opacity: 0.9;
}

.text {
  font-size: 36rpx;
  font-weight: 500;
}

.outfit-tip {
  font-size: 26rpx;
  opacity: 0.85;
  padding-top: 16rpx;
  border-top: 1rpx solid rgba(255,255,255,0.2);
}

.section-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.section-more {
  font-size: 26rpx;
  color: #3B82F6;
}

.course-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.course-item:last-child {
  border-bottom: none;
}

.course-time {
  width: 120rpx;
  font-size: 28rpx;
  color: #3B82F6;
  font-weight: 500;
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-name {
  font-size: 30rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.course-location {
  font-size: 24rpx;
  color: #999;
}

.cost-overview {
  display: flex;
  flex-direction: column;
}

.cost-amount {
  font-size: 48rpx;
  font-weight: 600;
  color: #FF6B6B;
  margin-bottom: 20rpx;
}

.cost-chart {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.chart-item {
  display: flex;
  align-items: center;
  min-width: 140rpx;
}

.chart-bar {
  height: 8rpx;
  background: #3B82F6;
  border-radius: 4rpx;
  margin-right: 8rpx;
}

.chart-label {
  font-size: 24rpx;
  color: #666;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
  font-size: 40rpx;
}

.action-name {
  font-size: 24rpx;
  color: #666;
}

.ai-card {
  background: linear-gradient(135deg, #11998E 0%, #38EF7D 100%);
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  color: #fff;
  margin-bottom: 24rpx;
}

.ai-content {
  flex: 1;
}

.ai-title {
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.ai-desc {
  font-size: 24rpx;
  opacity: 0.85;
}

.ai-arrow {
  font-size: 48rpx;
  opacity: 0.7;
}

.notification-bar {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
}

.notification-badge {
  width: 16rpx;
  height: 16rpx;
  background: #FF6B6B;
  border-radius: 50%;
  margin-right: 16rpx;
}

.notification-text {
  font-size: 28rpx;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 40rpx 0;
  color: #999;
  font-size: 28rpx;
}
</style>