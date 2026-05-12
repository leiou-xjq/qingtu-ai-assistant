<template>
  <view class="edit-page">
    <view class="header">
      <view class="header-left" @click="goBack">
        <text class="back-btn">‹</text>
      </view>
      <text class="title">编辑资料</text>
      <view class="header-right"></view>
    </view>

    <view class="form-content">
      <view class="form-card">
        <view class="form-item">
          <text class="label">用户名</text>
          <input class="input" :value="form.username" disabled />
        </view>

        <view class="form-item">
          <text class="label">昵称</text>
          <input class="input" v-model="form.nickname" placeholder="请输入昵称" />
        </view>

        <view class="form-item">
          <text class="label">性别</text>
          <view class="gender-select">
            <view
              v-for="item in genderOptions"
              :key="item.value"
              class="gender-option"
              :class="{ active: form.gender === item.value }"
              @click="form.gender = item.value"
            >
              {{ item.label }}
            </view>
          </view>
        </view>

        <view class="form-item">
          <text class="label">学校</text>
          <view class="school-input" @click="showSchoolPicker = true">
            <text :class="{ placeholder: !form.school }">{{ form.school || '点击选择学校' }}</text>
            <text class="arrow">›</text>
          </view>
        </view>

        <view class="form-item">
          <text class="label">地区</text>
          <input class="input" v-model="form.city" placeholder="请输入所在城市" />
        </view>
      </view>

      <view class="form-card">
        <view class="form-title">健康信息</view>

        <view class="form-item">
          <text class="label">年龄</text>
          <input class="input" type="number" v-model="form.age" placeholder="请输入年龄" />
        </view>

        <view class="form-item">
          <text class="label">身高(cm)</text>
          <input class="input" type="number" v-model="form.height" placeholder="请输入身高" />
        </view>

        <view class="form-item">
          <text class="label">体重(kg)</text>
          <input class="input" type="number" v-model="form.weight" placeholder="请输入体重" />
        </view>

        <view class="form-item">
          <text class="label">BMI</text>
          <view class="bmi-display">
            <text class="bmi-value">{{ calculatedBMI }}</text>
            <text class="bmi-status">{{ bmiStatus }}</text>
          </view>
        </view>

        <view class="form-item">
          <text class="label">活动系数</text>
          <view class="activity-select">
            <view
              v-for="item in activityLevels"
              :key="item.value"
              class="activity-option"
              :class="{ active: form.activityLevel === item.value }"
              @click="form.activityLevel = item.value"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="save-btn" @click="handleSave">保存</view>

    <!-- 学校选择弹窗 -->
    <view class="modal-mask" v-if="showSchoolPicker" @click="showSchoolPicker = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">选择学校</text>
          <text class="modal-close" @click="showSchoolPicker = false">×</text>
        </view>
        <view class="modal-body">
          <input
            class="search-input"
            type="text"
            v-model="schoolSearch"
            placeholder="搜索学校..."
            @input="onSearchInput"
          />
          <scroll-view class="school-list" scroll-y>
            <view
              v-for="(school, index) in schoolList"
              :key="index"
              class="school-item"
              :class="{ active: form.school === school.name }"
              @click="onSchoolTap(school)"
            >
              <text class="school-name">{{ school.name }}</text>
              <text class="school-location">{{ school.location }}</text>
            </view>
            <view v-if="schoolList.length === 0 && schoolSearch.length >= 2" class="empty-tip">
              未找到相关学校
            </view>
          </scroll-view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserInfo, updateUserInfo } from '@/api/user'
import { getHealthInfo, updateHealth } from '@/api/health'
import { searchSchool } from '@/api/school'

const form = ref({
  username: '',
  nickname: '',
  gender: 0,
  school: '',
  city: '',
  age: '',
  height: '',
  weight: '',
  activityLevel: 1.2
})

const genderOptions = [
  { value: 0, label: '保密' },
  { value: 1, label: '男' },
  { value: 2, label: '女' }
]

const activityLevels = [
  { value: 1.2, label: '久坐' },
  { value: 1.375, label: '轻度' },
  { value: 1.55, label: '中度' },
  { value: 1.75, label: '较重' },
  { value: 1.9, label: '重体力' }
]

const showSchoolPicker = ref(false)
const schoolSearch = ref('')
const schoolList = ref([])
const searchTimer = ref(null)

const calculatedBMI = computed(() => {
  const h = parseFloat(form.value.height)
  const w = parseFloat(form.value.weight)
  if (!h || !w || h <= 0 || w <= 0) return '--'
  const heightInM = h / 100
  const bmi = w / (heightInM * heightInM)
  return bmi.toFixed(1)
})

const bmiStatus = computed(() => {
  const bmi = parseFloat(calculatedBMI.value)
  if (isNaN(bmi)) return ''
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '偏胖'
  return '肥胖'
})

const goBack = () => {
  uni.navigateBack()
}

const onSearchInput = () => {
  clearTimeout(searchTimer.value)
  searchTimer.value = setTimeout(() => {
    searchSchools()
  }, 300)
}

const searchSchools = async () => {
  if (!schoolSearch.value || schoolSearch.value.length < 2) {
    schoolList.value = []
    return
  }
  try {
    const res = await searchSchool(schoolSearch.value)
    schoolList.value = res.data || []
  } catch (e) {
    console.log('搜索学校失败', e)
  }
}

const onSchoolTap = (school) => {
  if (!school || !school.name) return
  form.value.school = school.name
  schoolSearch.value = ''
  schoolList.value = []
  showSchoolPicker.value = false
  uni.showToast({ title: '已选择: ' + school.name, icon: 'success', duration: 1500 })
}

const handleSave = async () => {
  uni.showLoading({ title: '保存中...' })
  try {
    await updateUserInfo({
      nickname: form.value.nickname,
      gender: form.value.gender,
      school: form.value.school,
      city: form.value.city
    })

    await updateHealth({
      age: parseInt(form.value.age) || null,
      height: parseFloat(form.value.height) || null,
      weight: parseFloat(form.value.weight) || null,
      activityLevel: form.value.activityLevel
    })

    const currentUserInfo = uni.getStorageSync('userInfo') || {}
    uni.setStorageSync('userInfo', {
      ...currentUserInfo,
      nickname: form.value.nickname,
      gender: form.value.gender,
      school: form.value.school,
      city: form.value.city
    })

    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

onMounted(async () => {
  try {
    const res = await getUserInfo()
    if (res.data) {
      form.value.username = res.data.username || ''
      form.value.nickname = res.data.nickname || ''
      form.value.gender = res.data.gender || 0
      form.value.school = res.data.school || ''
      form.value.city = res.data.city || ''
    }
  } catch (e) {}

  try {
    const healthRes = await getHealthInfo()
    if (healthRes.data) {
      form.value.age = healthRes.data.age || ''
      form.value.height = healthRes.data.height || ''
      form.value.weight = healthRes.data.weight || ''
      form.value.activityLevel = healthRes.data.activityLevel || 1.2
    }
  } catch (e) {}
})
</script>

<style scoped>
.edit-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667EEA 0%, #764BA2 100%);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40rpx 32rpx 20rpx;
  color: #fff;
}

.header-left, .header-right {
  width: 80rpx;
}

.back-btn {
  font-size: 56rpx;
  color: #fff;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.form-content {
  padding: 24rpx 32rpx;
}

.form-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.1);
}

.form-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.form-item {
  margin-bottom: 28rpx;
}

.label {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 12rpx;
}

.input {
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 24rpx;
  font-size: 28rpx;
}

.input[disabled] {
  background: #e8e8e8;
  color: #999;
}

.school-input {
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 24rpx;
  font-size: 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.school-input .placeholder {
  color: #999;
}

.school-input .arrow {
  font-size: 36rpx;
  color: #999;
}

.gender-select {
  display: flex;
  gap: 16rpx;
}

.gender-option {
  flex: 1;
  padding: 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #666;
  text-align: center;
}

.gender-option.active {
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  color: #fff;
}

.activity-select {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.activity-option {
  padding: 12rpx 20rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #666;
}

.activity-option.active {
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  color: #fff;
}

.bmi-display {
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bmi-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

.bmi-status {
  font-size: 28rpx;
  color: #666;
}

.save-btn {
  margin: 32rpx;
  height: 88rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 44rpx;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.modal-content {
  width: 100%;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  max-height: 70vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #eee;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.modal-close {
  font-size: 48rpx;
  color: #999;
}

.modal-body {
  padding: 24rpx;
}

.search-input {
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 24rpx;
  font-size: 28rpx;
  margin-bottom: 16rpx;
}

.school-list {
  max-height: 400rpx;
}

.school-item {
  padding: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.school-item.active {
  background: #EEF2FF;
}

.school-name {
  font-size: 28rpx;
  color: #333;
}

.school-location {
  font-size: 24rpx;
  color: #999;
}

.empty-tip {
  text-align: center;
  padding: 40rpx;
  color: #999;
  font-size: 28rpx;
}
</style>