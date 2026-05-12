/**
 * 格式化工具
 */

// 格式化金额
export const formatMoney = (amount, decimals = 2) => {
  if (!amount) return '0.00'
  return Number(amount).toFixed(decimals)
}

// 格式化日期
export const formatDate = (date, format = 'YYYY-MM-DD') => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
}

// 格式化时间
export const formatTime = (date, format = 'HH:mm:ss') => {
  if (!date) return ''
  const d = new Date(date)
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

// 相对时间
export const formatRelativeTime = (date) => {
  if (!date) return ''
  const now = new Date()
  const d = new Date(date)
  const diff = now - d
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return formatDate(date)
}

// 格式化消费分类
export const formatCategory = (category) => {
  const map = {
    food: '饮食',
    transport: '交通',
    entertainment: '娱乐',
    shopping: '购物',
    life: '生活',
    study: '学习',
    other: '其他'
  }
  return map[category] || category
}

// 数字格式化
export const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return String(num)
}

export default {
  formatMoney,
  formatDate,
  formatTime,
  formatRelativeTime,
  formatCategory,
  formatNumber
}