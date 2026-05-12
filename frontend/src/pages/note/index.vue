<template>
  <view class="note-page">
    <!-- 笔记列表 -->
    <view class="note-list">
      <view class="note-card" v-for="note in notes" :key="note.id">
        <view class="note-header" @click="showNoteDetail(note)">
          <text class="course-name">{{ note.courseName }}</text>
          <text class="note-date">{{ note.classDate }}</text>
        </view>
        
        <view class="note-content" v-if="note.summary" @click="showNoteDetail(note)">
          <text class="summary-text">{{ note.summary }}</text>
        </view>

        <view class="note-footer">
          <text class="ai-model">🤖 AI生成</text>
          <view class="note-actions">
            <text class="view-btn" @click="showNoteDetail(note)">查看</text>
            <text class="delete-btn" @click="confirmDelete(note)">删除</text>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="notes.length === 0">
        <text>📝 暂无笔记记录</text>
        <text class="empty-tip">下课后自动生成课程笔记</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listNotes, deleteNote } from '@/api/note'

const notes = ref([])

const loadNotes = async () => {
  try {
    const res = await listNotes(1, 20)
    if (res.data && res.data.records) {
      notes.value = res.data.records.filter(n => 
        n.summary && !n.summary.includes('生成失败') && !n.summary.includes('基础模板')
      )
    }
  } catch (e) {
    console.error('加载笔记失败', e)
  }
}

const showNoteDetail = (note) => {
  uni.navigateTo({
    url: `/pages/note/detail?id=${note.id}`
  })
}

const confirmDelete = (note) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这条笔记吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteNote(note.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadNotes()
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onMounted(loadNotes)
</script>

<style scoped>
.note-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.note-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.course-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.note-date {
  font-size: 24rpx;
  color: #999;
}

.note-content {
  background: #f8fafc;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}

.summary-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.ai-model {
  font-size: 24rpx;
  color: #999;
}

.note-actions {
  display: flex;
  gap: 24rpx;
}

.view-btn {
  font-size: 26rpx;
  color: #3B82F6;
}

.delete-btn {
  font-size: 26rpx;
  color: #FF6B6B;
}

.empty-state {
  text-align: center;
  padding: 100rpx;
}

.empty-state text:first-child {
  font-size: 48rpx;
  display: block;
  margin-bottom: 16rpx;
}

.empty-tip {
  font-size: 26rpx;
  color: #999;
}
</style>
