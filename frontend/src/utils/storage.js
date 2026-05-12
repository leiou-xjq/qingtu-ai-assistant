/**
 * 本地存储工具
 */

// 设置本地存储
export const setStorage = (key, value) => {
  try {
    uni.setStorageSync(key, value)
    return true
  } catch (e) {
    console.error('setStorage error:', e)
    return false
  }
}

// 获取本地存储
export const getStorage = (key, defaultValue = null) => {
  try {
    const value = uni.getStorageSync(key)
    return value !== '' ? value : defaultValue
  } catch (e) {
    console.error('getStorage error:', e)
    return defaultValue
  }
}

// 删除本地存储
export const removeStorage = (key) => {
  try {
    uni.removeStorageSync(key)
    return true
  } catch (e) {
    return false
  }
}

// 清空本地存储
export const clearStorage = () => {
  try {
    uni.clearStorageSync()
    return true
  } catch (e) {
    return false
  }
}

// 本地存储封装
export const storage = {
  set: setStorage,
  get: getStorage,
  remove: removeStorage,
  clear: clearStorage
}

export default storage