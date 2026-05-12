<template>
  <view class="login-page">
    <view class="login-header">
      <text class="logo-text">青途智伴</text>
      <text class="slogan">大学生AI生活助手</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <input type="text" v-model="formData.username" placeholder="请输入用户名" />
      </view>
      <view class="form-item">
        <input type="password" v-model="formData.password" placeholder="请输入密码" />
      </view>
    </view>

    <view class="login-btn" @click="handleLogin">登录</view>
    
    <view class="register-link">
      <text>还没有账号？</text>
      <text class="link" @click="goRegister">立即注册</text>
    </view>

    <!-- 演示账号提示 -->
    <view class="demo-tip">
      <text>演示账号：admin / 123456</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { login } from '@/api/user'

const formData = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!formData.value.username) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!formData.value.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }

  try {
    const res = await login(formData.value)
    if (res.data && res.data.token) {
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('userInfo', res.data)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 1000)
    }
  } catch (e) {
    console.log('登录失败', e)
  }
}

const goRegister = () => {
  uni.navigateTo({ url: '/pages/auth/register' })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #667EEA 0%, #764BA2 100%);
  padding: 100rpx 48rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  text-align: center;
  color: #fff;
  margin-bottom: 80rpx;
}

.logo-text {
  font-size: 56rpx;
  font-weight: 700;
  display: block;
  margin-bottom: 16rpx;
}

.slogan {
  font-size: 28rpx;
  opacity: 0.85;
}

.login-form {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx;
  box-shadow: 0 20rpx 60rpx rgba(0,0,0,0.1);
}

.form-item {
  margin-bottom: 32rpx;
}

.form-item input {
  height: 96rpx;
  border: 2rpx solid #eee;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.form-item input:focus {
  border-color: #667EEA;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 48rpx;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 48rpx;
}

.login-btn:active {
  opacity: 0.9;
}

.register-link {
  margin-top: 32rpx;
  font-size: 28rpx;
  color: #fff;
}

.link {
  margin-left: 8rpx;
  text-decoration: underline;
}

.demo-tip {
  margin-top: 48rpx;
  font-size: 24rpx;
  color: rgba(255,255,255,0.6);
}
</style>