<template>
  <view class="course-page">
    <!-- 周次选择 -->
    <view class="week-selector">
      <view class="week-nav">
        <text class="week-label">第{{ currentWeek }}周</text>
        <view class="week-btns">
          <text class="prev" @click="changeWeek(-1)">‹</text>
          <text class="week-date">{{ weekDateRange }}</text>
          <text class="next" @click="changeWeek(1)">›</text>
        </view>
      </view>
    </view>

    <!-- 星期标签 -->
    <view class="weekday-tabs">
      <view class="weekday-tab" 
            v-for="day in weekdays" 
            :key="day.value" 
            :class="{ active: selectedWeekday === day.value }"
            @click="selectedWeekday = day.value">
        <text class="day-name">{{ day.name }}</text>
        <text class="day-count" v-if="getDayCourseCount(day.value) > 0">{{ getDayCourseCount(day.value) }}</text>
      </view>
    </view>

    <!-- 课程卡片 -->
    <view class="course-list">
      <view class="course-card" 
            v-for="course in filteredCourses" 
            :key="course.id"
            @click="editCourse(course)"
            :style="{ borderLeftColor: course.color || '#3B82F6' }">
        <view class="course-time">
          <text class="time-text">{{ course.startTime }}</text>
          <text class="time-line">-</text>
          <text class="time-text">{{ course.endTime }}</text>
        </view>
        <view class="course-info">
          <text class="course-name">{{ course.name }}</text>
          <view class="course-meta">
            <text class="meta-item">📍 {{ course.location }}</text>
            <text class="meta-item" v-if="course.teacher">👨 {{ course.teacher }}</text>
          </view>
        </view>
        <view class="course-actions">
          <switch :checked="course.reminderEnabled === 1" @change="toggleReminder(course)" />
        </view>
      </view>

      <view class="empty-state" v-if="filteredCourses.length === 0">
        <text>🎉 今日无课程，好好休息吧~</text>
      </view>
    </view>

    <!-- 功能按钮 -->
    <view class="action-btns">
      <view class="action-btn" @click="showAddCourse">
        <text>添加课程</text>
      </view>
      <view class="action-btn secondary" @click="importCourse">
        <text>导入课表</text>
      </view>
      <view class="action-btn secondary" @click="showSemesterSettings">
        <text>学期设置</text>
      </view>
    </view>

    <!-- 添加/编辑课程弹窗 -->
    <view class="modal-mask" v-if="showModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ isEdit ? '编辑课程' : '添加课程' }}</text>
          <text class="modal-close" @click="closeModal">×</text>
        </view>
        
        <view class="modal-body">
          <view class="form-item">
            <text class="form-label">课程名称 *</text>
            <input class="form-input" v-model="courseForm.name" placeholder="请输入课程名称" />
          </view>
          
          <view class="form-item">
            <text class="form-label">授课教师</text>
            <input class="form-input" v-model="courseForm.teacher" placeholder="请输入教师姓名" />
          </view>
          
          <view class="form-item">
            <text class="form-label">上课地点</text>
            <input class="form-input" v-model="courseForm.location" placeholder="请输入上课地点" />
          </view>
          
          <view class="form-item">
            <text class="form-label">星期 *</text>
            <picker :range="weekdayOptions" @change="onWeekdayChange">
              <view class="form-picker">
                {{ weekdayOptions[courseForm.weekday - 1] || '请选择' }}
              </view>
            </picker>
          </view>
          
          <view class="form-row">
            <view class="form-item">
              <text class="form-label">开始时间 *</text>
              <picker mode="time" :value="courseForm.startTime" @change="onStartTimeChange">
                <view class="form-picker">{{ courseForm.startTime || '请选择' }}</view>
              </picker>
            </view>
            <view class="form-item">
              <text class="form-label">结束时间 *</text>
              <picker mode="time" :value="courseForm.endTime" @change="onEndTimeChange">
                <view class="form-picker">{{ courseForm.endTime || '请选择' }}</view>
              </picker>
            </view>
          </view>
          
          <view class="form-row">
            <view class="form-item">
              <text class="form-label">起始周</text>
              <picker mode="selector" :value="Number(courseForm.weekStart || 1) - 1" :range="weekRange" @change="onWeekStartChange">
                <view class="form-picker">第{{ courseForm.weekStart }}周</view>
              </picker>
            </view>
            <view class="form-item">
              <text class="form-label">结束周</text>
              <picker mode="selector" :value="Number(courseForm.weekEnd || 20) - 1" :range="weekRange" @change="onWeekEndChange">
                <view class="form-picker">第{{ courseForm.weekEnd }}周</view>
              </picker>
            </view>
          </view>
          
          <view class="form-item">
            <text class="form-label">课程类型</text>
            <picker :range="courseTypeOptions" @change="onCourseTypeChange">
              <view class="form-picker">
                {{ courseTypeOptions[courseForm.courseTypeIndex] || '必修' }}
              </view>
            </picker>
          </view>
          
          <view class="form-item">
            <text class="form-label">提醒设置</text>
            <view class="reminder-row">
              <switch :checked="courseForm.reminderEnabled === 1" @change="onReminderChange" />
              <text class="reminder-text" v-if="courseForm.reminderEnabled === 1">{{ courseForm.reminderMinutes }}分钟前</text>
            </view>
          </view>
        </view>
        
        <view class="modal-footer">
          <view class="btn-delete" v-if="isEdit" @click="deleteCourse">删除</view>
          <view class="btnCancel" @click="closeModal">取消</view>
          <view class="btnSave" @click="saveCourse">保存</view>
        </view>
      </view>
    </view>

    <!-- 学期设置弹窗 -->
    <view class="modal-mask" v-if="showSemesterModal" @click="closeSemesterModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">学期设置</text>
          <text class="modal-close" @click="closeSemesterModal">×</text>
        </view>
        
        <view class="modal-body">
          <view class="form-item">
            <text class="form-label">开学日期</text>
            <picker mode="date" :value="semesterForm.semesterStart" @change="onSemesterStartChange">
              <view class="form-picker">{{ semesterForm.semesterStart || '请选择' }}</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="form-label">教学周数</text>
            <picker mode="selector" :value="semesterForm.totalWeeks - 1" :range="weekOptions" @change="onTotalWeeksChange">
              <view class="form-picker">第{{ semesterForm.totalWeeks }}周</view>
            </picker>
          </view>
        </view>
        
        <view class="modal-footer">
          <view class="btnCancel" @click="closeSemesterModal">取消</view>
          <view class="btnSave" @click="saveSemesterSettings">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getWeekSchedule, setCourseReminder, addCourse, updateCourse, deleteCourse as deleteCourseApi, importSchedule } from '@/api/course'
import { getUserInfo, updateUserInfo } from '@/api/user'

const currentWeek = ref(1)
const weekDateRange = ref('')
const selectedWeekday = ref(new Date().getDay() || 7)
const schedule = ref({})

const weekdays = [
  { value: 1, name: '周一' },
  { value: 2, name: '周二' },
  { value: 3, name: '周三' },
  { value: 4, name: '周四' },
  { value: 5, name: '周五' },
  { value: 6, name: '周六' },
  { value: 7, name: '周日' }
]

const weekdayOptions = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const courseTypeOptions = ['必修', '选修', '公选']
const weekRange = ['第1周', '第2周', '第3周', '第4周', '第5周', '第6周', '第7周', '第8周', '第9周', '第10周', '第11周', '第12周', '第13周', '第14周', '第15周', '第16周', '第17周', '第18周', '第19周', '第20周']
const weekOptions = Array.from({ length: 20 }, (_, i) => `第${i + 1}周`)

const showModal = ref(false)
const showSemesterModal = ref(false)
const isEdit = ref(false)
const courseForm = ref({
  id: null,
  name: '',
  teacher: '',
  location: '',
  weekday: 1,
  startTime: '',
  endTime: '',
  weekStart: 1,
  weekEnd: 20,
  courseType: 'required',
  courseTypeIndex: 0,
  reminderEnabled: 0,
  reminderMinutes: 15
})
const semesterForm = ref({
  semesterStart: '',
  totalWeeks: 20
})

const filteredCourses = computed(() => {
  if (!schedule.value.schedule) return []
  const courses = schedule.value.schedule[selectedWeekday.value] || []
  return mergeCoursesByName(courses)
})

const mergeCoursesByName = (courses) => {
  if (!courses || courses.length === 0) return []
  
  const courseMap = {}
  for (const course of courses) {
    const key = `${course.name}_${course.location}_${course.teacher}`
    if (!courseMap[key]) {
      courseMap[key] = {
        ...course,
        timeRanges: []
      }
    }
    courseMap[key].timeRanges.push({
      startTime: course.startTime,
      endTime: course.endTime
    })
  }
  
  const merged = Object.values(courseMap).map(course => {
    course.timeRanges.sort((a, b) => a.startTime.localeCompare(b.startTime))
    return {
      ...course,
      startTime: course.timeRanges[0].startTime,
      endTime: course.timeRanges[course.timeRanges.length - 1].endTime
    }
  })
  
  return merged
}

const getDayCourseCount = (weekday) => {
  if (!schedule.value.schedule) return 0
  const courses = schedule.value.schedule[weekday] || []
  const merged = mergeCoursesByName(courses)
  return merged.length
}

const changeWeek = async (delta) => {
  currentWeek.value += delta
  if (currentWeek.value < 1) currentWeek.value = 1
  if (currentWeek.value > 20) currentWeek.value = 20
  await loadSchedule()
}

const loadSchedule = async () => {
  try {
    const res = await getWeekSchedule(currentWeek.value)
    if (res.data) {
      schedule.value = res.data
      currentWeek.value = res.data.weekNum || 1
      weekDateRange.value = res.data.weekDateRange || ''
    }
  } catch (e) {
    console.log('加载课表失败')
  }
}

const toggleReminder = async (course) => {
  try {
    const newEnabled = course.reminderEnabled === 1 ? 0 : 1
    await setCourseReminder(course.id, newEnabled === 1, 15)
    course.reminderEnabled = newEnabled
    uni.showToast({ title: '设置成功', icon: 'success' })
  } catch (e) {}
}

const showAddCourse = () => {
  isEdit.value = false
  courseForm.value = {
    id: null,
    name: '',
    teacher: '',
    location: '',
    weekday: selectedWeekday.value,
    startTime: '08:00',
    endTime: '09:40',
    weekStart: 1,
    weekEnd: 20,
    courseType: 'required',
    courseTypeIndex: 0,
    reminderEnabled: 0,
    reminderMinutes: 15
  }
  showModal.value = true
}

const editCourse = (course) => {
  isEdit.value = true
  courseForm.value = {
    id: course.id,
    name: course.name,
    teacher: course.teacher || '',
    location: course.location || '',
    weekday: course.weekday,
    startTime: course.startTime,
    endTime: course.endTime,
    weekStart: course.weekStart || 1,
    weekEnd: course.weekEnd || 20,
    courseType: course.courseType || 'required',
    courseTypeIndex: course.courseType === 'elective' ? 1 : course.courseType === 'public' ? 2 : 0,
    reminderEnabled: course.reminderEnabled || 0,
    reminderMinutes: course.reminderMinutes || 15
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const onWeekdayChange = (e) => {
  courseForm.value.weekday = e.detail.value + 1
}

const onStartTimeChange = (e) => {
  courseForm.value.startTime = e.detail.value
}

const onEndTimeChange = (e) => {
  courseForm.value.endTime = e.detail.value
}

const onWeekStartChange = (e) => {
  courseForm.value.weekStart = e.detail.value + 1
}

const onWeekEndChange = (e) => {
  courseForm.value.weekEnd = e.detail.value + 1
}

const onCourseTypeChange = (e) => {
  courseForm.value.courseTypeIndex = e.detail.value
  courseForm.value.courseType = e.detail.value === 1 ? 'elective' : e.detail.value === 2 ? 'public' : 'required'
}

const onReminderChange = (e) => {
  courseForm.value.reminderEnabled = e.detail.value ? 1 : 0
}

const showSemesterSettings = async () => {
  try {
    const res = await getUserInfo()
    if (res.data) {
      semesterForm.value = {
        semesterStart: res.data.semesterStart || '',
        totalWeeks: res.data.totalWeeks || 20
      }
    }
    showSemesterModal.value = true
  } catch (e) {
    console.log('获取用户信息失败', e)
  }
}

const closeSemesterModal = () => {
  showSemesterModal.value = false
}

const onSemesterStartChange = (e) => {
  const date = new Date()
  date.setFullYear(2000 + Math.floor(e.detail.value.slice(0, 2)))
  const parts = e.detail.value.split('-')
  if (parts.length === 3) {
    semesterForm.value.semesterStart = e.detail.value
  }
}

const onTotalWeeksChange = (e) => {
  semesterForm.value.totalWeeks = e.detail.value + 1
}

const saveSemesterSettings = async () => {
  try {
    await updateUserInfo({
      semesterStart: semesterForm.value.semesterStart,
      totalWeeks: semesterForm.value.totalWeeks
    })
    uni.showToast({ title: '保存成功', icon: 'success' })
    closeSemesterModal()
    await loadSchedule()
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const saveCourse = async () => {
  if (!courseForm.value.name) {
    uni.showToast({ title: '请输入课程名称', icon: 'none' })
    return
  }
  if (!courseForm.value.startTime) {
    uni.showToast({ title: '请选择开始时间', icon: 'none' })
    return
  }
  if (!courseForm.value.endTime) {
    uni.showToast({ title: '请选择结束时间', icon: 'none' })
    return
  }
  
  try {
    const data = {
      name: courseForm.value.name,
      teacher: courseForm.value.teacher,
      location: courseForm.value.location,
      weekday: courseForm.value.weekday,
      startTime: courseForm.value.startTime,
      endTime: courseForm.value.endTime,
      weekStart: courseForm.value.weekStart,
      weekEnd: courseForm.value.weekEnd,
      courseType: courseForm.value.courseType,
      reminderEnabled: courseForm.value.reminderEnabled,
      reminderMinutes: courseForm.value.reminderMinutes
    }
    
    if (isEdit.value) {
      await updateCourse(courseForm.value.id, data)
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await addCourse(data)
      uni.showToast({ title: '添加成功', icon: 'success' })
    }
    closeModal()
    await loadSchedule()
  } catch (e) {
    console.log('保存课程失败', e)
  }
}

const deleteCourse = async () => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这门课程吗?',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteCourseApi(courseForm.value.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          closeModal()
          await loadSchedule()
        } catch (e) {
          console.log('删除失败', e)
        }
      }
    }
  })
}

const importCourse = () => {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['xlsx', 'xls', 'pdf', 'doc', 'docx', 'txt'],
    success: async (res) => {
      const file = res.tempFiles[0]
      try {
        uni.showLoading({ title: '导入�?..' })
        await importSchedule(file.path)
        uni.hideLoading()
        uni.showToast({ title: '导入成功', icon: 'success' })
        await loadSchedule()
      } catch (e) {
        uni.hideLoading()
        console.log('导入失败', e)
      }
    },
    fail: () => {
      // 用户取消
    }
  })
}

onMounted(async () => {
  await initCurrentWeek()
  await loadSchedule()
})

const initCurrentWeek = async () => {
  try {
    const res = await getUserInfo()
    if (res.data && res.data.semesterStart) {
      const semesterStart = new Date(res.data.semesterStart)
      const today = new Date()
      const diffTime = today - semesterStart
      const diffWeeks = Math.floor(diffTime / (1000 * 60 * 60 * 24 * 7)) + 1
      currentWeek.value = Math.min(Math.max(diffWeeks, 1), res.data.totalWeeks || 20)
      uni.setStorageSync('semesterStart', res.data.semesterStart)
      uni.setStorageSync('totalWeeks', res.data.totalWeeks || 20)
    }
  } catch (e) {
    const localStart = uni.getStorageSync('semesterStart')
    if (localStart) {
      const semesterStart = new Date(localStart)
      const today = new Date()
      const diffTime = today - semesterStart
      const diffWeeks = Math.floor(diffTime / (1000 * 60 * 60 * 24 * 7)) + 1
      const totalWeeks = uni.getStorageSync('totalWeeks') || 20
      currentWeek.value = Math.min(Math.max(diffWeeks, 1), totalWeeks)
    }
  }
  
  selectedWeekday.value = new Date().getDay() || 7
}
</script>

<style scoped>
.course-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx;
  padding-bottom: 200rpx;
}

.week-selector {
  margin-bottom: 24rpx;
}

.week-nav {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.week-label {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 12rpx;
}

.week-btns {
  display: flex;
  align-items: center;
}

.prev, .next {
  width: 56rpx;
  height: 56rpx;
  background: #fff;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  color: #3B82F6;
}

.week-date {
  font-size: 28rpx;
  color: #333;
  margin: 0 24rpx;
}

.weekday-tabs {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;
}

.weekday-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 8rpx;
  border-radius: 16rpx;
  position: relative;
}

.weekday-tab.active {
  background: #3B82F6;
}

.day-name {
  font-size: 26rpx;
  color: #333;
}

.weekday-tab.active .day-name {
  color: #fff;
}

.day-count {
  width: 32rpx;
  height: 32rpx;
  background: #FF6B6B;
  border-radius: 16rpx;
  font-size: 22rpx;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 6rpx;
}

.course-list {
  margin-bottom: 24rpx;
}

.course-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  display: flex;
  border-left: 6rpx solid #3B82F6;
}

.course-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 24rpx;
  min-width: 100rpx;
}

.time-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.time-line {
  font-size: 24rpx;
  color: #999;
  margin: 4rpx 0;
}

.course-info {
  flex: 1;
}

.course-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #666;
}

.course-actions {
  display: flex;
  align-items: center;
}

.action-btns {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 24rpx;
  padding: 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
}

.action-btn {
  flex: 1;
  background: #3B82F6;
  color: #fff;
  text-align: center;
  padding: 28rpx;
  border-radius: 24rpx;
}

.action-btn.secondary {
  background: #fff;
  color: #3B82F6;
  border: 2rpx solid #3B82F6;
}

.empty-state {
  text-align: center;
  padding: 80rpx;
  color: #999;
  font-size: 28rpx;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.modal-content {
  width: 100%;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  max-height: 85vh;
  overflow-y: auto;
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
  font-size: 40rpx;
  color: #999;
}

.modal-body {
  padding: 24rpx 32rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.form-label {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
  display: block;
}

.form-input {
  background: #f5f5f5;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
}

.form-picker {
  background: #f5f5f5;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 28rpx;
  color: #333;
}

.form-row {
  display: flex;
  gap: 24rpx;
}

.form-row .form-item {
  flex: 1;
}

.reminder-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.reminder-text {
  font-size: 24rpx;
  color: #666;
}

.modal-footer {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 32rpx 48rpx;
  border-top: 1rpx solid #eee;
}

.btn-delete {
  background: #FF6B6B;
  color: #fff;
  text-align: center;
  padding: 24rpx;
  border-radius: 24rpx;
  flex: 1;
}

.btnCancel {
  background: #f5f5f5;
  color: #666;
  text-align: center;
  padding: 24rpx;
  border-radius: 24rpx;
  flex: 1;
}

.btnSave {
  background: #3B82F6;
  color: #fff;
  text-align: center;
  padding: 24rpx;
  border-radius: 24rpx;
  flex: 1;
}
</style>