<template>
  <view class="weather-page">
    <!-- 实时天气 -->
    <view class="weather-main">
      <view class="location">
        <text class="city">{{ weather.cityName || '北京' }}</text>
        <text class="update-time">更新于 {{ weather.updateTime || '刚刚' }}</text>
      </view>
      
      <view class="temp-display">
        <text class="temp">{{ weather.temp || '25' }}°</text>
        <text class="weather-text">{{ weather.text || '多云' }}</text>
      </view>

      <view class="weather-detail">
        <view class="detail-item">
          <text class="detail-value">{{ weather.feelsLike || '26' }}°</text>
          <text class="detail-label">体感温度</text>
        </view>
        <view class="detail-item">
          <text class="detail-value">{{ weather.uvIndex || '中等' }}</text>
          <text class="detail-label">紫外线</text>
        </view>
      </view>
    </view>

    <!-- 穿搭建议 -->
    <view class="outfit-card" v-if="outfitSuggestion">
      <view class="card-header">
        <text class="card-title">👗 今日穿搭建议</text>
      </view>
      <view class="outfit-content">
        <view class="outfit-lines">
          <text 
            v-for="(line, index) in outfitLines" 
            :key="index" 
            class="outfit-line"
          >{{ line }}</text>
        </view>
      </view>
    </view>

    <!-- 推送设置 -->
    <view class="push-card">
      <text class="card-title">📬 早安推送设置</text>
      <view class="push-toggle">
        <text>每日天气穿搭推送</text>
        <switch :checked="pushEnabled" @change="togglePush" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentWeather, getForecast, getOutfitSuggestion } from '@/api/weather'

const weather = ref({})
const outfitSuggestion = ref('')
const forecast = ref([])
const pushEnabled = ref(true)

// 穿搭建议按行分割
const outfitLines = ref([])
const updateOutfitLines = () => {
  if (outfitSuggestion.value) {
    outfitLines.value = outfitSuggestion.value.split('\n').filter(line => line.trim())
  }
}

const togglePush = () => {
  pushEnabled.value = !pushEnabled.value
  uni.showToast({ title: pushEnabled.value ? '已开启' : '已关闭', icon: 'success' })
}

onMounted(async () => {
  const userInfo = uni.getStorageSync('userInfo')
  const city = userInfo?.city || '北京'
  
  try {
    const res = await getCurrentWeather(city)
    if (res.data) weather.value = res.data
  } catch (e) {}

  try {
    const outfitRes = await getOutfitSuggestion()
    if (outfitRes.data) {
      outfitSuggestion.value = outfitRes.data
      updateOutfitLines()
    }
  } catch (e) {}

  try {
    const forecastRes = await getForecast(city, 7)
    if (forecastRes.data) forecast.value = forecastRes.data
  } catch (e) {}
})
</script>

<style scoped>
.weather-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667EEA 0%, #764BA2 100%);
  padding: 24rpx;
}

.weather-main {
  background: rgba(255,255,255,0.15);
  border-radius: 32rpx;
  padding: 40rpx;
  color: #fff;
  margin-bottom: 24rpx;
}

.location {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.city {
  font-size: 40rpx;
  font-weight: 600;
  margin-right: 16rpx;
}

.update-time {
  font-size: 24rpx;
  opacity: 0.7;
}

.temp-display {
  text-align: center;
  margin-bottom: 32rpx;
}

.temp {
  font-size: 120rpx;
  font-weight: 200;
  display: block;
}

.weather-text {
  font-size: 36rpx;
  margin-top: 8rpx;
}

.weather-detail {
  display: flex;
  justify-content: space-around;
}

.detail-item {
  text-align: center;
}

.detail-value {
  font-size: 32rpx;
  font-weight: 600;
  display: block;
  margin-bottom: 8rpx;
}

.detail-label {
  font-size: 24rpx;
  opacity: 0.8;
}

.outfit-card, .forecast-card, .push-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
  display: block;
}

.outfit-content {
  background: #f8fafc;
  border-radius: 16rpx;
  padding: 24rpx;
}

.outfit-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.outfit-lines {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.outfit-line {
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
  padding: 8rpx 0;
  border-bottom: 1rpx dashed #e0e0e0;
}

.outfit-line:last-child {
  border-bottom: none;
}

.forecast-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.forecast-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.forecast-item:last-child {
  border-bottom: none;
}

.forecast-date {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
}

.forecast-text {
  flex: 1;
  font-size: 28rpx;
  color: #666;
}

.forecast-temp {
  font-size: 28rpx;
  color: #3B82F6;
}

.push-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 28rpx;
  color: #333;
}
</style>