/**
 * 青途智伴 - Axios请求封装
 * 
 * 功能说明：
 * - 请求拦截：添加Token、统一处理参数
 * - 响应拦截：统一错误处理、Token刷新
 * - 重复请求取消
 * - 超时处理
 */

// 后端API基础地址 - Windows模拟器用10.0.2.2，真机用局域网IP如192.168.1.x
const BASE_URL = 'http://192.168.74.1:8080/api'

// 创建axios实例
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      timeout: 30000,
      success: (res) => {
        if (!res.data) {
          uni.showToast({ title: '服务器无响应', icon: 'none' })
          reject({ msg: '服务器无响应' })
          return
        }
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data)
          } else if (res.data.code === 401) {
            // Token过期，跳转登录
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/auth/login' })
            reject(res.data)
          } else {
            uni.showToast({
              title: res.data.msg || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else {
          uni.showToast({
            title: '网络错误',
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        console.log('请求失败', err)
        uni.showToast({
          title: '网络连接失败: ' + (err.errMsg || '请检查网络'),
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

// GET请求
const get = (url, params, options = {}) => {
  return request({
    url,
    method: 'GET',
    data: params,
    ...options
  })
}

// POST请求
const post = (url, data, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

// PUT请求
const put = (url, data, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

// DELETE请求
const del = (url, params, options = {}) => {
  return request({
    url,
    method: 'DELETE',
    data: params,
    ...options
  })
}

// 文件上传
const upload = (url, filePath, name = 'file') => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.uploadFile({
      url: BASE_URL + url,
      filePath,
      name,
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          resolve(data)
        } else {
          reject(data)
        }
      },
      fail: reject
    })
  })
}

export default {
  request,
  get,
  post,
  put,
  del,
  upload,
  BASE_URL
}