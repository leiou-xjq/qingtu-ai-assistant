<template>
  <view class="detail-page">
    <view class="header">
      <view class="back-btn" @click="goBack">← 返回</view>
    </view>
    
    <view class="content" v-if="note">
      <text class="title">{{ note.title }}</text>
      <text class="time">创建于 {{ note.createTime }}</text>
      
      <view class="markdown-content">
        <rich-text :nodes="formattedContent"></rich-text>
      </view>
    </view>
    
    <view class="loading" v-else>
      <text>加载中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getNoteDetail } from '@/api/note'

const note = ref(null)

onLoad((options) => {
  if (options.id) {
    loadNoteDetail(options.id)
  }
})

const loadNoteDetail = async (id) => {
  try {
    const res = await getNoteDetail(id)
    if (res.data) {
      note.value = res.data
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const formattedContent = computed(() => {
  if (!note.value || !note.value.content) return ''
  return note.value.content
    .replace(/\n/g, '<br>')
    .replace(/\*\s?([^*]+)\*/g, '<strong>$1</strong>')
    .replace(/#\s?([^#]+)/g, '<h2>$1</h2>')
    .replace(/##\s?([^#]+)/g, '<h3>$1</h3>')
    .replace(/- /g, '• ')
})

const goBack = () => {
  uni.navigateBack()
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #F8FAFC;
}

.header {
  padding: 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
}

.back-btn {
  font-size: 28rpx;
  color: #3B82F6;
}

.content {
  padding: 24rpx;
}

.title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.time {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-bottom: 32rpx;
}

.markdown-content {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  line-height: 1.8;
}

.loading {
  text-align: center;
  padding: 100rpx;
  color: #999;
}
</style>
