/**
 * 消费记录相关API
 */
import request from '../utils/request'

// 获取消费记录列表
export const getCostList = (params) => request.get('/cost/list', params)

// 添加消费记录
export const addCostRecord = (data) => request.post('/cost/add', data)

// 删除消费记录
export const deleteCostRecord = (id) => request.delete(`/cost/${id}`)

// 导入账单文件
export const importBillFile = (filePath, source) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: request.BASE_URL + '/cost/import',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      formData: { source: source || 'wechat' },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          resolve(data)
        } else {
          uni.showToast({ title: data.msg || '导入失败', icon: 'none' })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '导入失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

// OCR识别账单
export const ocrBill = (filePath) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: request.BASE_URL + '/cost/ocr',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          resolve(data)
        } else {
          uni.showToast({ title: data.msg || '识别失败', icon: 'none' })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '识别失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 获取月度统计
export const getMonthlyStatistics = (year, month) => request.get('/cost/monthly', { year, month })

// 获取分类统计
export const getCategoryStatistics = (params) => request.get('/cost/category', params)

// AI月度报告
export const getMonthlyReport = (year, month) => request.get('/cost/report', { year, month })