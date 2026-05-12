<template>
  <view class="register-page">
    <view class="register-header">
      <text class="logo-text">青途智伴</text>
      <text class="slogan">创建你的账号</text>
    </view>

    <view class="register-form">
      <!-- 步骤1: 账号信息 -->
      <view v-show="currentStep === 1">
        <view class="form-title">账号信息</view>
        <view class="form-item">
          <input type="text" v-model="formData.username" placeholder="用户名（3-20位）" />
        </view>
        <view class="form-item">
          <input type="text" v-model="formData.nickname" placeholder="昵称（选填）" />
        </view>
        <view class="form-item">
          <input type="password" v-model="formData.password" placeholder="密码（6-20位）" />
        </view>
        <view class="form-item">
          <input type="password" v-model="formData.confirmPassword" placeholder="确认密码" />
        </view>
      </view>

      <!-- 步骤2: 选择学校 -->
      <view v-show="currentStep === 2">
        <view class="form-title">选择学校</view>
        <view class="form-item">
          <input
            type="text"
            v-model="searchInput"
            placeholder="搜索学校..."
            @input="onSearchInput"
          />
        </view>
        <view class="school-list" v-if="showSchoolList">
          <view
            v-for="(school, index) in schoolList"
            :key="index"
            class="school-item"
            :class="{ active: formData.school === school.name }"
            @tap="onSchoolTap(school)"
          >
            <text class="school-name">{{ school.name }}</text>
            <text class="school-location">{{ school.location }}</text>
          </view>
        </view>
      </view>

      <!-- 步骤3: 定位地区 -->
      <view v-show="currentStep === 3">
        <view class="form-title">定位地区</view>
        <view class="form-item">
          <input type="text" v-model="formData.city" placeholder="请输入所在城市" />
        </view>
      </view>

      <!-- 步骤4: 健康信息 -->
      <view v-show="currentStep === 4">
        <view class="form-title">健康信息</view>
        <view class="form-item">
          <picker mode="selector" :range="genderOptions" @change="onGenderChange">
            <view class="picker-value">
              {{ formData.gender ? (formData.gender === 'M' ? '男' : '女') : '选择性别' }}
            </view>
          </picker>
        </view>
        <view class="form-item">
          <input type="number" v-model="formData.age" placeholder="年龄" />
        </view>
        <view class="form-item">
          <input type="number" v-model="formData.height" placeholder="身高(cm)" />
        </view>
        <view class="form-item">
          <input type="number" v-model="formData.weight" placeholder="体重(kg)" />
        </view>
        <view class="form-item">
          <picker mode="selector" :range="activityOptions" range-key="label" @change="onActivityChange">
            <view class="picker-value">
              {{ selectedActivityLabel || '选择活动水平' }}
            </view>
          </picker>
        </view>
      </view>
    </view>

    <!-- 进度条 -->
    <view class="progress-container">
      <view class="progress-bar">
        <view class="progress" :style="{ width: (currentStep / 4 * 100) + '%' }"></view>
      </view>
      <text class="step-text">步骤 {{ currentStep }}/4</text>
    </view>

    <!-- 按钮区域 -->
    <view class="btn-area">
      <view v-if="currentStep > 1" class="btn btn-secondary" @click="prevStep">上一步</view>
      <view v-if="currentStep < 4" class="btn btn-primary" @click="nextStep">下一步</view>
      <view v-if="currentStep === 4" class="btn btn-primary" @click="handleRegister" :loading="registering">注册</view>
    </view>

    <!-- 返回登录 -->
    <view class="back-link">
      <text>已有账号？</text>
      <text class="link" @click="goBack">返回登录</text>
    </view>
  </view>
</template>

<script>
import { register } from '@/api/user'
import { searchSchool } from '@/api/school'

export default {
  data() {
    return {
      currentStep: 1,
      searchInput: '',
      schoolList: [],
      showSchoolList: false,
      registering: false,
      formData: {
        username: '',
        nickname: '',
        password: '',
        confirmPassword: '',
        school: '',
        city: '',
        gender: '',
        age: '',
        height: '',
        weight: '',
        activityLevel: 1.2
      },
      genderOptions: ['男', '女'],
      activityOptions: [
        { value: 1.2, label: '久坐（基本不动）' },
        { value: 1.375, label: '轻度（每周1-3天）' },
        { value: 1.55, label: '中等（每周3-5天）' },
        { value: 1.75, label: '活跃（每周6-7天）' },
        { value: 1.9, label: '高活跃（运动员）' }
      ],
      searchTimer: null
    }
  },
  computed: {
    selectedActivityLabel() {
      const item = this.activityOptions.find(o => o.value === this.formData.activityLevel)
      return item ? item.label : ''
    }
  },
  methods: {
    goBack() {
      uni.redirectTo({
        url: '/pages/auth/login'
      })
    },
    validateStep1() {
      if (!this.formData.username || this.formData.username.length < 3) {
        uni.showToast({ title: '用户名至少3位', icon: 'none' })
        return false
      }
      if (!this.formData.password || this.formData.password.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return false
      }
      if (this.formData.password !== this.formData.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return false
      }
      return true
    },
    validateStep2() {
      if (!this.formData.school) {
        uni.showToast({ title: '请选择学校', icon: 'none' })
        return false
      }
      return true
    },
    validateStep3() {
      if (!this.formData.city) {
        uni.showToast({ title: '请输入所在城市', icon: 'none' })
        return false
      }
      return true
    },
    validateStep4() {
      if (!this.formData.gender) {
        uni.showToast({ title: '请选择性别', icon: 'none' })
        return false
      }
      if (!this.formData.age) {
        uni.showToast({ title: '请输入年龄', icon: 'none' })
        return false
      }
      return true
    },
    nextStep() {
      if (this.currentStep === 1 && !this.validateStep1()) return
      if (this.currentStep === 2 && !this.validateStep2()) return
      if (this.currentStep === 3 && !this.validateStep3()) return
      this.currentStep++
    },
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },
    onSearchInput(e) {
      this.searchInput = e.detail.value
      clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.searchSchools()
      }, 300)
    },
    async searchSchools() {
      if (!this.searchInput || this.searchInput.length < 2) {
        this.schoolList = []
        this.showSchoolList = false
        return
      }
      try {
        const res = await searchSchool(this.searchInput)
        this.schoolList = res.data || []
        this.showSchoolList = this.schoolList.length > 0
      } catch (e) {
        console.log('搜索学校失败', e)
      }
    },
    onSchoolTap(school) {
      if (!school || !school.name) {
        return
      }
      this.formData.school = school.name
      this.searchInput = school.name
      this.schoolList = []
      this.showSchoolList = false
      uni.showToast({ title: '已选择: ' + school.name, icon: 'success', duration: 1500 })
    },
    onGenderChange(e) {
      const genderMap = { '0': 'M', '1': 'F' }
      this.formData.gender = genderMap[e.detail.value] || 'F'
    },
    onActivityChange(e) {
      this.formData.activityLevel = this.activityOptions[e.detail.value].value
    },
    async handleRegister() {
      if (!this.validateStep4()) return

      this.registering = true
      try {
        const res = await register({
          username: this.formData.username,
          nickname: this.formData.nickname,
          password: this.formData.password,
          school: this.formData.school,
          city: this.formData.city,
          gender: this.formData.gender,
          age: parseInt(this.formData.age),
          height: parseFloat(this.formData.height),
          weight: parseFloat(this.formData.weight),
          activityLevel: this.formData.activityLevel
        })

        if (res.code === 200) {
          uni.setStorageSync('token', res.data.token)
          uni.setStorageSync('userInfo', res.data)
          uni.showToast({ title: '注册成功', icon: 'success' })
          setTimeout(() => {
            uni.switchTab({ url: '/pages/index/index' })
          }, 1500)
        } else {
          uni.showToast({ title: res.message || '注册失败', icon: 'none' })
        }
      } catch (e) {
        console.error('注册失败:', e)
        uni.showToast({ title: '注册失败，请稍后重试', icon: 'none' })
      } finally {
        this.registering = false
      }
    }
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667EEA 0%, #764BA2 100%);
  padding: 100rpx 48rpx 40rpx;
}

.register-header {
  text-align: center;
  color: #fff;
  margin-bottom: 60rpx;
}

.logo-text {
  font-size: 48rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 16rpx;
}

.slogan {
  font-size: 28rpx;
  opacity: 0.85;
}

.register-form {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(0,0,0,0.15);
}

.form-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 32rpx;
}

.form-item {
  margin-bottom: 28rpx;
}

.form-item input,
.form-item .picker-value {
  height: 88rpx;
  padding: 0 24rpx;
  background: #f5f5f5;
  border-radius: 16rpx;
  font-size: 28rpx;
}

.form-item .picker-value {
  display: flex;
  align-items: center;
}

.school-list {
  max-height: 300rpx;
  overflow-y: auto;
  background: #f5f5f5;
  border-radius: 16rpx;
  padding: 8rpx;
}

.school-item {
  padding: 24rpx;
  border-bottom: 1rpx solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.school-item:last-child {
  border-bottom: none;
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

.progress-container {
  margin-top: 32rpx;
  text-align: center;
}

.progress-bar {
  height: 8rpx;
  background: rgba(255,255,255,0.3);
  border-radius: 4rpx;
  overflow: hidden;
  margin-bottom: 12rpx;
}

.progress {
  height: 100%;
  background: #fff;
  border-radius: 4rpx;
  transition: width 0.3s ease;
}

.step-text {
  font-size: 24rpx;
  color: rgba(255,255,255,0.8);
}

.btn-area {
  display: flex;
  gap: 24rpx;
  margin-top: 32rpx;
}

.btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 44rpx;
  font-size: 32rpx;
}

.btn-primary {
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  color: #fff;
}

.btn-secondary {
  background: rgba(255,255,255,0.2);
  color: #fff;
  border: 1rpx solid rgba(255,255,255,0.3);
}

.back-link {
  margin-top: 32rpx;
  text-align: center;
  font-size: 28rpx;
  color: rgba(255,255,255,0.8);
}

.link {
  margin-left: 8rpx;
  text-decoration: underline;
}
</style>