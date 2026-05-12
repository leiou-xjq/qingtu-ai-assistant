<template>
  <view class="diet-page">
    <!-- 营养进度条 -->
    <view class="progress-card">
      <view class="progress-header">
        <text class="progress-title">今日热量摄入</text>
        <text class="progress-refresh" @click="loadProgress">🔄</text>
      </view>
      <view class="progress-content">
        <text class="consumed">{{ calorieProgress.consumedCalories || 0 }}</text>
        <text class="divider">/</text>
        <text class="target">{{ calorieProgress.dailyTarget || 1800 }}千卡</text>
      </view>
      <view class="progress-bar-container">
        <view class="progress-bar-fill" :style="{ width: calorieProgress.progressPercent + '%' }"></view>
      </view>
      <view class="progress-tip">
        <text>剩余 {{ calorieProgress.remainingCalories || 1800 }} 千卡</text>
      </view>
    </view>

    <!-- AI今日推荐 -->
    <view class="recommendation-section">
      <view class="section-header">
        <text class="section-title">🍽️ AI今日食谱</text>
        <text class="refresh-btn" @click="refreshRecommendation">🔄 换一批</text>
      </view>

      <view class="ai-loading" v-if="loading">
        <text>AI正在生成推荐...</text>
      </view>

      <view v-else-if="aiRecommendation" class="recommendation-list">
        <!-- 早餐 -->
        <view class="meal-card">
          <view class="meal-header">
            <text class="meal-icon">🌅</text>
            <text class="meal-name">早餐</text>
            <text class="meal-cal">约{{ aiRecommendation.breakfast?.calories || 500 }}千卡</text>
          </view>
          <view class="meal-foods" v-if="aiRecommendation.breakfast?.foods?.length">
            <text v-for="(food, idx) in aiRecommendation.breakfast.foods" :key="idx" class="food-tag">{{ food }}</text>
          </view>
          <view class="meal-eat-btn" :class="{ disabled: calorieProgress.meals?.breakfast?.recorded }" @click="showIntakeModal('breakfast')">
            <text>{{ calorieProgress.meals?.breakfast?.recorded ? '✓ 已完成' : '我吃了这个' }}</text>
          </view>
        </view>

        <!-- 午餐 -->
        <view class="meal-card">
          <view class="meal-header">
            <text class="meal-icon">☀️</text>
            <text class="meal-name">午餐</text>
            <text class="meal-cal">约{{ aiRecommendation.lunch?.calories || 700 }}千卡</text>
          </view>
          <view class="meal-foods" v-if="aiRecommendation.lunch?.foods?.length">
            <text v-for="(food, idx) in aiRecommendation.lunch.foods" :key="idx" class="food-tag">{{ food }}</text>
          </view>
          <view class="meal-eat-btn" :class="{ disabled: calorieProgress.meals?.lunch?.recorded }" @click="showIntakeModal('lunch')">
            <text>{{ calorieProgress.meals?.lunch?.recorded ? '✓ 已完成' : '我吃了这个' }}</text>
          </view>
        </view>

        <!-- 晚餐 -->
        <view class="meal-card">
          <view class="meal-header">
            <text class="meal-icon">🌙</text>
            <text class="meal-name">晚餐</text>
            <text class="meal-cal">约{{ aiRecommendation.dinner?.calories || 500 }}千卡</text>
          </view>
          <view class="meal-foods" v-if="aiRecommendation.dinner?.foods?.length">
            <text v-for="(food, idx) in aiRecommendation.dinner.foods" :key="idx" class="food-tag">{{ food }}</text>
          </view>
          <view class="meal-eat-btn" :class="{ disabled: calorieProgress.meals?.dinner?.recorded }" @click="showIntakeModal('dinner')">
            <text>{{ calorieProgress.meals?.dinner?.recorded ? '✓ 已完成' : '我吃了这个' }}</text>
          </view>
        </view>

        <!-- 建议 -->
        <view class="tips-card" v-if="aiRecommendation.tips?.length">
          <text class="tips-title">💡 建议</text>
          <view class="tips-list">
            <text v-for="(tip, idx) in aiRecommendation.tips" :key="idx" class="tip-item">{{ tip }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty-state" @click="refreshRecommendation">
        <text>点击获取AI个性化食谱推荐</text>
      </view>
    </view>

    <!-- 记录弹窗 -->
    <view class="modal" v-if="showModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <text class="modal-title">{{ getMealName(currentMealType) }} 记录</text>
        <view class="form-item">
          <text class="form-label">吃了什么？</text>
          <input class="form-input" v-model="foodInput" placeholder="描述你吃的食物，如：一碗牛肉面" />
        </view>
        <view class="estimate-tip" v-if="estimateLoading">
          <text>AI正在估算热量...</text>
        </view>
        <view class="estimate-result" v-else-if="estimateResult">
          <text class="result-label">估算热量：</text>
          <text class="result-value">{{ estimateResult }} 千卡</text>
        </view>
        <view class="modal-btns">
          <view class="modal-btn cancel" @click="closeModal">取消</view>
          <view class="modal-btn confirm" @click="confirmIntake">记录</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHealthKpi } from '@/api/health'
import { getTodayRecommendation } from '@/api/dish'
import { recordIntake, getTodayProgress } from '@/api/calorie'

const healthInfo = ref({})
const aiRecommendation = ref(null)
const loading = ref(false)

// 热量进度
const calorieProgress = ref({
  consumedCalories: 0,
  dailyTarget: 1800,
  progressPercent: 0,
  remainingCalories: 1800
})

// 记录弹窗
const showModal = ref(false)
const currentMealType = ref('')
const foodInput = ref('')
const estimateLoading = ref(false)
const estimateResult = ref(0)

const getBmiColor = (level) => {
  switch (level) {
    case '正常': return '#10B981'
    case '偏瘦': return '#F59E0B'
    case '偏胖': return '#F59E0B'
    case '肥胖': return '#EF4444'
    default: return '#999'
  }
}

const getBmiProgress = (bmi) => {
  if (!bmi) return '0%'
  const percent = Math.min(Math.max((bmi - 15) / 20 * 100, 0), 100)
  return percent + '%'
}

const getMealName = (type) => {
  const map = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
  return map[type] || ''
}

const goHealthProfile = () => {
  uni.navigateTo({ url: '/pages/profile/index' })
}

const loadProgress = async () => {
  try {
    const res = await getTodayProgress()
    if (res.data) {
      calorieProgress.value = {
        consumedCalories: res.data.consumedCalories || 0,
        dailyTarget: res.data.dailyTarget || 1800,
        progressPercent: res.data.progressPercent || 0,
        remainingCalories: res.data.remainingCalories || 1800,
        meals: res.data.meals || {}
      }
    }
  } catch (e) {
    console.log('加载进度失败', e)
  }
}

const refreshRecommendation = async () => {
  loading.value = true
  try {
    const res = await getTodayRecommendation()
    if (res.data) {
      const data = res.data
      try {
        let parsed = null
        if (typeof data === 'string') {
          const jsonMatch = data.match(/\{[\s\S]*\}/)
          if (jsonMatch) {
            parsed = JSON.parse(jsonMatch[0])
          }
        } else {
          parsed = data
        }

        if (parsed && typeof parsed === 'object') {
          aiRecommendation.value = {
            breakfast: parsed.breakfast || { name: '', calories: '', foods: [] },
            lunch: parsed.lunch || { name: '', calories: '', foods: [] },
            dinner: parsed.dinner || { name: '', calories: '', foods: [] },
            tips: parsed.tips || []
          }
        }
      } catch (e) {
        console.log('解析失败', e)
        aiRecommendation.value = null
      }
    }
  } catch (e) {
    uni.showToast({ title: '获取推荐失败', icon: 'none' })
  }
  loading.value = false
}

const showIntakeModal = (mealType) => {
  if (calorieProgress.value.meals?.[mealType]?.recorded) {
    uni.showToast({ title: '今日已记录过此餐', icon: 'none' })
    return
  }
  currentMealType.value = mealType
  foodInput.value = ''
  estimateResult.value = 0
  showModal.value = true

  // 预填充AI推荐的食材
  if (aiRecommendation.value && aiRecommendation.value[mealType]?.foods) {
    foodInput.value = aiRecommendation.value[mealType].foods.join('，')
  }
}

const closeModal = () => {
  showModal.value = false
}

const confirmIntake = async () => {
  if (!foodInput.value.trim()) {
    uni.showToast({ title: '请输入吃了什么', icon: 'none' })
    return
  }

  estimateLoading.value = true
  try {
    const res = await recordIntake(currentMealType.value, foodInput.value)
    estimateLoading.value = false

    if (res.code === 200 && res.data) {
      estimateResult.value = res.data.estimatedCalories
      uni.showToast({ title: `已记录 ${res.data.estimatedCalories} 千卡`, icon: 'success' })

      setTimeout(() => {
        closeModal()
        loadProgress()
      }, 1500)
    } else if (res.code !== 200) {
      uni.showToast({ title: res.msg || '记录失败', icon: 'none' })
    }
  } catch (e) {
    estimateLoading.value = false
    uni.showToast({ title: '记录失败', icon: 'none' })
  }
}

onMounted(async () => {
  try {
    const res = await getHealthKpi()
    if (res.data) healthInfo.value = res.data
  } catch (e) {}

  await loadProgress()
  await refreshRecommendation()
})
</script>

<style scoped>
.diet-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx;
}

.progress-card {
  background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
  border-radius: 24rpx;
  padding: 32rpx;
  color: #fff;
  margin-bottom: 24rpx;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.progress-title {
  font-size: 28rpx;
  opacity: 0.9;
}

.progress-refresh {
  font-size: 26rpx;
}

.progress-content {
  display: flex;
  align-items: baseline;
  margin-bottom: 16rpx;
}

.consumed {
  font-size: 56rpx;
  font-weight: 700;
}

.divider {
  font-size: 36rpx;
  margin: 0 12rpx;
  opacity: 0.7;
}

.target {
  font-size: 32rpx;
  opacity: 0.9;
}

.progress-bar-container {
  height: 16rpx;
  background: rgba(255,255,255,0.3);
  border-radius: 8rpx;
  margin-bottom: 12rpx;
}

.progress-bar-fill {
  height: 100%;
  background: #fff;
  border-radius: 8rpx;
  transition: width 0.3s ease;
}

.progress-tip {
  font-size: 24rpx;
  opacity: 0.8;
  text-align: right;
}

.recommendation-section {
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
}

.refresh-btn {
  font-size: 26rpx;
  color: #3B82F6;
}

.ai-loading {
  text-align: center;
  padding: 48rpx;
  color: #666;
}

.meal-card {
  background: #F8FAFC;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}

.meal-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.meal-icon {
  font-size: 36rpx;
  margin-right: 12rpx;
}

.meal-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.meal-cal {
  font-size: 26rpx;
  color: #FF6B6B;
}

.meal-foods {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.food-tag {
  padding: 10rpx 20rpx;
  background: #E8F5E9;
  border-radius: 24rpx;
  font-size: 26rpx;
  color: #333;
}

.meal-eat-btn {
  background: #10B981;
  color: #fff;
  text-align: center;
  padding: 16rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.meal-eat-btn.disabled {
  background: #9CA3AF;
}

.tips-card {
  background: #FEF3C7;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-top: 16rpx;
}

.tips-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #92400E;
  display: block;
  margin-bottom: 12rpx;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.tip-item {
  font-size: 26rpx;
  color: #92400E;
}

.empty-state {
  text-align: center;
  padding: 48rpx;
  color: #999;
  background: #F5F5F5;
  border-radius: 16rpx;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-content {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  display: block;
  margin-bottom: 32rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.form-input {
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.estimate-tip {
  text-align: center;
  padding: 24rpx;
  color: #666;
  font-size: 26rpx;
}

.estimate-result {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
  background: #E8F5E9;
  border-radius: 12rpx;
  margin-bottom: 24rpx;
}

.result-label {
  font-size: 28rpx;
  color: #333;
}

.result-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #10B981;
  margin-left: 12rpx;
}

.modal-btns {
  display: flex;
  gap: 24rpx;
}

.modal-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
}

.modal-btn.cancel {
  background: #f5f5f5;
  color: #666;
}

.modal-btn.confirm {
  background: #10B981;
  color: #fff;
}
</style>