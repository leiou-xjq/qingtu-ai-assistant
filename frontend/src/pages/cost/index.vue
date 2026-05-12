<template>
  <view class="cost-page">
    <!-- 月份切换 -->
    <view class="month-selector">
      <view class="month-btn" @click="changeMonth(-1)">‹</view>
      <text class="month-text">{{ year }}年{{ month }}月</text>
      <view class="month-btn" @click="changeMonth(1)">›</view>
    </view>

    <!-- 消费概览 -->
    <view class="cost-overview-card">
      <view class="total-amount">
        <text class="label">本月支出</text>
        <text class="amount">¥{{ stats.totalAmount || '0.00' }}</text>
      </view>
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ stats.recordCount || 0 }}</text>
          <text class="stat-label">笔消费</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ stats.dailyAverage || '0' }}</text>
          <text class="stat-label">日均</text>
        </view>
      </view>
    </view>

    <!-- 分类统计 -->
    <view class="category-card">
      <text class="card-title">消费分类</text>
      <view class="category-list">
        <view class="category-item" v-for="item in categoryStats" :key="item.category">
          <view class="category-info">
            <text class="category-name">{{ getCategoryName(item.category) }}</text>
            <text class="category-amount">¥{{ item.amount }}</text>
          </view>
          <view class="category-bar">
            <view class="bar-fill" :style="{ width: item.percentage + '%' }"></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 消费记录列表 -->
    <view class="record-list">
      <view class="record-header">
        <text class="card-title">消费记录</text>
        <text class="add-btn" @click="showAddModal">+ 添加</text>
      </view>
      
      <view class="record-item" v-for="record in records" :key="record.id">
        <view class="record-icon">{{ getCategoryIcon(record.category) }}</view>
        <view class="record-info">
          <text class="record-merchant">{{ record.merchantName || getCategoryName(record.category) }}</text>
          <text class="record-time">{{ formatTime(record.tradeTime) }}</text>
        </view>
        <text class="record-amount">-¥{{ record.amount }}</text>
      </view>

      <view class="empty-state" v-if="records.length === 0">
        <text>暂无消费记录</text>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="action-btns">
      <view class="action-btn primary" @click="showAddModal">
        <text class="btn-icon">➕</text>
        <text class="btn-text">记一笔</text>
      </view>
      <view class="action-btn" @click="getMonthlyReport">
        <text class="btn-icon">📊</text>
        <text class="btn-text">上月分析</text>
      </view>
    </view>

    <!-- 添加记录弹窗 -->
    <view class="modal" v-if="showModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <text class="modal-title">记一笔</text>
        <view class="form-item">
          <text class="form-label">消费金额 <text class="required">*</text></text>
          <input class="form-input amount-input" type="digit" v-model="addForm.amount" placeholder="0.00" />
        </view>
        <view class="form-item">
          <text class="form-label">消费分类 <text class="required">*</text></text>
          <picker mode="selector" :range="categoryOptions" @change="onCategoryChange">
            <view class="picker-value">{{ addForm.categoryName || '选择分类' }}</view>
          </picker>
        </view>
        <view class="form-item">
          <text class="form-label">商户名称</text>
          <input class="form-input" v-model="addForm.merchantName" placeholder="选填" />
        </view>
        <view class="form-item">
          <text class="form-label">消费时间</text>
          <picker mode="date" :value="addForm.tradeTime" @change="onTimeChange">
            <view class="picker-value">{{ addForm.tradeTime || '选择日期' }}</view>
          </picker>
        </view>
        <view class="form-item">
          <text class="form-label">备注</text>
          <input class="form-input" v-model="addForm.remark" placeholder="选填" />
        </view>
        <view class="modal-btns">
          <view class="modal-btn cancel" @click="closeModal">取消</view>
          <view class="modal-btn confirm" @click="confirmAdd">确定</view>
        </view>
      </view>
    </view>

    <!-- AI分析报告弹窗 -->
    <view class="modal" v-if="showReportModal" @click="closeReportModal">
      <view class="modal-content report-modal" @click.stop>
        <text class="modal-title">{{ reportYear }}年{{ reportMonth }}月消费分析</text>
        <scroll-view class="report-content" scroll-y="true">
          <view v-if="reportLoading" class="report-loading">
            <text>AI正在分析中...</text>
          </view>
          <view v-else-if="monthlyReport" class="report-body">
            <view class="report-section">
              <text class="section-title">💰 消费概览</text>
              <view class="report-item">{{ monthlyReport.overview }}</view>
            </view>
            <view class="report-section" v-if="monthlyReport.structure">
              <text class="section-title">📊 消费结构</text>
              <view class="report-item" v-for="(item, idx) in monthlyReport.structure" :key="idx">{{ item }}</view>
            </view>
            <view class="report-section" v-if="monthlyReport.abnormal">
              <text class="section-title">⚠️ 异常提醒</text>
              <view class="report-item abnormal">{{ monthlyReport.abnormal }}</view>
            </view>
            <view class="report-section" v-if="monthlyReport.suggestions">
              <text class="section-title">💡 优化建议</text>
              <view class="report-item suggestion" v-for="(item, idx) in monthlyReport.suggestions" :key="idx">{{ item }}</view>
            </view>
          </view>
          <view v-else class="report-empty">
            <text>暂无分析数据</text>
          </view>
        </scroll-view>
        <view class="modal-btns">
          <view class="modal-btn confirm" @click="closeReportModal">关闭</view>
        </view>
      </view>
    </view>

    </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMonthlyStatistics, getCostList, addCostRecord, getMonthlyReport as getReport } from '@/api/cost'

const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const reportYear = ref(year.value)
const reportMonth = ref(month.value)
const stats = ref({})
const categoryStats = ref([])
const records = ref([])
const showModal = ref(false)
const showReportModal = ref(false)
const reportLoading = ref(false)
const monthlyReport = ref(null)

const addForm = ref({
  amount: '',
  category: 'food',
  categoryName: '饮食',
  merchantName: '',
  tradeTime: new Date().toISOString().split('T')[0],
  remark: ''
})

const categoryOptions = ['饮食', '交通', '娱乐', '购物', '生活', '学习', '其他']
const categoryMap = {
  food: { name: '饮食', icon: '🍜' },
  transport: { name: '交通', icon: '🚇' },
  entertainment: { name: '娱乐', icon: '🎮' },
  shopping: { name: '购物', icon: '🛒' },
  life: { name: '生活', icon: '🏠' },
  study: { name: '学习', icon: '📚' },
  other: { name: '其他', icon: '📦' }
}
const categoryIndexMap = {
  '饮食': 'food',
  '交通': 'transport',
  '娱乐': 'entertainment',
  '购物': 'shopping',
  '生活': 'life',
  '学习': 'study',
  '其他': 'other'
}

const getCategoryName = (category) => categoryMap[category]?.name || category
const getCategoryIcon = (category) => categoryMap[category]?.icon || '📦'

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString()
}

const changeMonth = async (delta) => {
  month.value += delta
  if (month.value > 12) {
    month.value = 1
    year.value++
  } else if (month.value < 1) {
    month.value = 12
    year.value--
  }
  await loadData()
}

const loadData = async () => {
  try {
    const statsRes = await getMonthlyStatistics(year.value, month.value)
    if (statsRes.data) {
      stats.value = statsRes.data
      const catStats = statsRes.data.categoryStats || {}
      const total = Object.values(catStats).reduce((sum, val) => sum + Number(val), 0)
      categoryStats.value = Object.entries(catStats).map(([cat, amount]) => ({
        category: cat,
        amount: Number(amount).toFixed(2),
        percentage: total > 0 ? (Number(amount) / total * 100).toFixed(1) : 0
      }))
    }
  } catch (e) {
    console.log('获取统计失败', e)
  }

  try {
    const listRes = await getCostList({ page: 1, size: 20, year: year.value, month: month.value })
    if (listRes.data && listRes.data.records) {
      records.value = listRes.data.records
    }
  } catch (e) {
    console.log('获取列表失败', e)
  }
}

const showAddModal = () => {
  addForm.value = {
    amount: '',
    category: 'food',
    categoryName: '饮食',
    merchantName: '',
    tradeTime: new Date().toISOString().split('T')[0],
    remark: ''
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const onCategoryChange = (e) => {
  const name = categoryOptions[e.detail.value]
  addForm.value.categoryName = name
  addForm.value.category = categoryIndexMap[name]
}

const onTimeChange = (e) => {
  addForm.value.tradeTime = e.detail.value
}

const confirmAdd = async () => {
  if (!addForm.value.amount) {
    uni.showToast({ title: '请输入金额', icon: 'none' })
    return
  }
  if (!addForm.value.category) {
    uni.showToast({ title: '请选择分类', icon: 'none' })
    return
  }

  try {
    await addCostRecord({
      amount: parseFloat(addForm.value.amount),
      category: addForm.value.category,
      merchantName: addForm.value.merchantName || '',
      tradeTime: addForm.value.tradeTime,
      remark: addForm.value.remark || ''
    })
    uni.showToast({ title: '添加成功', icon: 'success' })
    closeModal()
    await loadData()
  } catch (e) {
    uni.showToast({ title: '添加失败', icon: 'none' })
  }
}

const getMonthlyReport = async () => {
  const isCurrentMonth = month.value === new Date().getMonth() + 1 && year.value === new Date().getFullYear()
  const lastMonthVal = isCurrentMonth ? (month.value === 1 ? 12 : month.value - 1) : month.value
  const lastYearVal = isCurrentMonth ? (month.value === 1 ? year.value - 1 : year.value) : year.value
  reportYear.value = lastYearVal
  reportMonth.value = lastMonthVal

  showReportModal.value = true
  reportLoading.value = true
  monthlyReport.value = null

  try {
    const res = await getReport(lastYearVal, lastMonthVal)
    reportLoading.value = false
    if (res.data) {
      monthlyReport.value = res.data
    } else {
      monthlyReport.value = { overview: '上月暂无消费记录' }
    }
  } catch (e) {
    reportLoading.value = false
    monthlyReport.value = { overview: '获取分析报告失败，请重试' }
  }
}

const closeReportModal = () => {
  showReportModal.value = false
  monthlyReport.value = null
}

onMounted(loadData)
</script>

<style scoped>
.cost-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx;
}

.month-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.month-btn {
  width: 64rpx;
  height: 64rpx;
  background: #fff;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #3B82F6;
}

.month-text {
  font-size: 32rpx;
  font-weight: 600;
  margin: 0 32rpx;
}

.cost-overview-card {
  background: linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%);
  border-radius: 24rpx;
  padding: 32rpx;
  color: #fff;
  margin-bottom: 24rpx;
}

.total-amount {
  text-align: center;
  margin-bottom: 24rpx;
}

.total-amount .label {
  font-size: 28rpx;
  opacity: 0.85;
}

.total-amount .amount {
  font-size: 64rpx;
  font-weight: 700;
}

.stats-row {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32rpx;
  font-weight: 600;
  display: block;
}

.stat-label {
  font-size: 24rpx;
  opacity: 0.85;
}

.category-card, .record-list {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.category-item {
  margin-bottom: 20rpx;
}

.category-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.category-name {
  font-size: 28rpx;
  color: #333;
}

.category-amount {
  font-size: 28rpx;
  color: #FF6B6B;
}

.category-bar {
  height: 8rpx;
  background: #f0f0f0;
  border-radius: 4rpx;
}

.bar-fill {
  height: 100%;
  background: #3B82F6;
  border-radius: 4rpx;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.add-btn {
  color: #3B82F6;
  font-size: 28rpx;
}

.record-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.record-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}

.record-info {
  flex: 1;
}

.record-merchant {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 4rpx;
}

.record-time {
  font-size: 24rpx;
  color: #999;
}

.record-amount {
  font-size: 30rpx;
  color: #FF6B6B;
  font-weight: 500;
}

.action-btns {
  display: flex;
  gap: 24rpx;
}

.action-btn {
  flex: 1;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.action-btn.primary {
  background: #3B82F6;
  color: #fff;
}

.btn-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.btn-text {
  font-size: 28rpx;
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
  max-height: 80vh;
  overflow-y: auto;
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

.picker-value {
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 0 24rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}

.modal-btns {
  display: flex;
  gap: 24rpx;
  margin-top: 32rpx;
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
  background: #3B82F6;
  color: #fff;
}

.report-modal {
  width: 650rpx;
  max-height: 85vh;
}

.report-content {
  max-height: 600rpx;
  margin-bottom: 32rpx;
}

.report-loading {
  text-align: center;
  padding: 60rpx;
  color: #666;
  font-size: 28rpx;
}

.report-body {
  padding: 0 10rpx;
}

.report-section {
  margin-bottom: 32rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.report-item {
  font-size: 26rpx;
  color: #555;
  line-height: 1.6;
  padding: 8rpx 0;
}

.report-item.abnormal {
  color: #FF6B6B;
  background: #FFF5F5;
  padding: 16rpx;
  border-radius: 12rpx;
}

.report-item.suggestion {
  color: #3B82F6;
  background: #F0F7FF;
  padding: 16rpx;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}

.report-empty {
  text-align: center;
  padding: 60rpx;
  color: #999;
  font-size: 28rpx;
}

.required {
  color: #FF6B6B;
}

.amount-input {
  font-size: 40rpx;
  font-weight: 600;
  text-align: center;
  letter-spacing: 2rpx;
}

.empty-state {
  text-align: center;
  padding: 60rpx;
  color: #999;
  font-size: 28rpx;
}
</style>