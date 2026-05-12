/**
 * Pinia 用户状态管理
 */
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: uni.getStorageSync('userInfo') || null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userId: (state) => state.userInfo?.id,
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username
  },

  actions: {
    setToken(token) {
      this.token = token
      uni.setStorageSync('token', token)
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo
      uni.setStorageSync('userInfo', userInfo)
    },

    logout() {
      this.token = ''
      this.userInfo = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
})