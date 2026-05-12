<template>
  <view class="chat-page">
    <!-- 侧边会话列表 -->
    <view class="session-sidebar" v-if="showSidebar">
      <view class="sidebar-header">
        <text class="sidebar-title">历史会话</text>
        <view class="new-chat-btn" @click="createNewSession">+ 新对话</view>
      </view>
      <scroll-view scroll-y class="session-list">
        <view
          class="session-item"
          v-for="session in sessionList"
          :key="session.id"
          :class="{ active: currentSessionId === session.id }"
        >
          <view class="session-info" @click="selectSession(session)">
            <text class="session-title">{{ session.title }}</text>
            <text class="session-time">{{ formatTime(session.updatedAt) }}</text>
          </view>
          <view class="session-actions">
            <text class="edit-btn" @click.stop="renameCurrentSession(session)">✏️</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 主聊天区域 -->
    <view class="chat-main">
      <!-- 顶部栏 -->
      <view class="chat-header">
        <view class="header-left" v-if="!showSidebar" @click="showSidebar = true">
          <text class="menu-icon">☰</text>
        </view>
        <text class="chat-title">{{ currentTitle || 'AI助手' }}</text>
        <view class="header-right" v-if="currentSessionId && showSidebar" @click="showSidebar = false">
          <text class="close-icon">×</text>
        </view>
        <view class="header-right" v-else-if="currentSessionId" @click="deleteCurrentSession">
          <text class="delete-icon">🗑️</text>
        </view>
      </view>

      <!-- 聊天消息列表 -->
      <scroll-view class="chat-list" scroll-y :scroll-top="scrollTop">
        <view class="message-item" v-for="(msg, index) in messages" :key="index" :class="msg.role">
          <view class="message-avatar" v-if="msg.role === 'assistant'">🤖</view>
          <view class="message-content">
            <text class="message-text" decode="true">{{ msg.content }}</text>
            <text class="message-time">{{ msg.createdAt }}</text>
          </view>
        </view>

        <!-- 欢迎消息 -->
        <view class="welcome-message" v-if="messages.length === 0 && currentSessionId">
          <text class="welcome-title">👋 你好，我是青途智伴AI助手</text>
          <text class="welcome-desc">我可以帮你：
• 解答校园生活问题
• 规划学习安排
• 推荐健康饮食
• 分析消费情况

有什么可以帮你的吗？</text>
        </view>

        <!-- 空状态 -->
        <view class="empty-state" v-if="messages.length === 0 && !currentSessionId">
          <text class="empty-text">请选择或创建一个会话开始聊天</text>
        </view>
      </scroll-view>

      <!-- 文件预览 -->
      <view class="file-preview" v-if="selectedFile">
        <view class="file-info">
          <text class="file-icon">📄</text>
          <text class="file-name">{{ selectedFile.name }}</text>
        </view>
        <text class="file-remove" @click="removeFile">×</text>
      </view>

      <!-- 输入区域 -->
      <view class="input-area">
        <view class="upload-btn" @click="chooseFile">
          <text>📎</text>
        </view>
        <input type="text" v-model="inputText" placeholder="输入问题..." @confirm="sendMessage" />
        <view class="send-btn" @click="sendMessage" :class="{ disabled: isLoading }">
          <text v-if="!isLoading">发送</text>
          <text v-else>...</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { ragAskStream, ragAskStreamWithFile, getSessionList, getSessionHistory, createSession, deleteSession, renameSession } from '@/api/rag'

const messages = ref([])
const sessionList = ref([])
const inputText = ref('')
const scrollTop = ref(0)
const isLoading = ref(false)
const currentSessionId = ref(null)
const currentTitle = ref('')
const showSidebar = ref(false)
const selectedFile = ref(null)
const uploadUrl = ref('')

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString()
}

const loadSessionList = async () => {
  try {
    const res = await getSessionList()
    if (res.data) {
      sessionList.value = res.data
    }
  } catch (e) {
    console.log('获取会话列表失败', e)
  }
}

const selectSession = async (session) => {
  currentSessionId.value = session.id
  currentTitle.value = session.title
  showSidebar.value = false

  try {
    const res = await getSessionHistory(session.id)
    if (res.data) {
      messages.value = res.data
    }
  } catch (e) {
    console.log('获取会话历史失败', e)
  }
}

const createNewSession = async () => {
  try {
    const res = await createSession('')
    if (res.data) {
      await loadSessionList()
      selectSession({ id: res.data.id, title: res.data.title })
    }
  } catch (e) {
    console.log('创建会话失败', e)
  }
}

const deleteCurrentSession = async () => {
  if (!currentSessionId.value) return

  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个会话吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteSession(currentSessionId.value)
          currentSessionId.value = null
          currentTitle.value = ''
          messages.value = []
          await loadSessionList()
        } catch (e) {
          console.log('删除会话失败', e)
        }
      }
    }
  })
}

const renameCurrentSession = async (session) => {
  uni.showModal({
    title: '重命名会话',
    editable: true,
    placeholderText: '请输入新名称',
    success: async (res) => {
      if (res.confirm && res.content) {
        try {
          await renameSession(session.id, res.content)
          await loadSessionList()
          if (currentSessionId.value === session.id) {
            currentTitle.value = res.content
          }
        } catch (e) {
          console.log('重命名会话失败', e)
          uni.showToast({ title: '重命名失败', icon: 'none' })
        }
      }
    }
  })
}

const chooseFile = () => {
  uni.chooseMessageFile({
    count: 1,
    success: (res) => {
      const file = res.tempFiles[0]
      const ext = file.name.split('.').pop().toLowerCase()
      const allowedExts = ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt']
      if (!allowedExts.includes(ext)) {
        uni.showToast({ title: '不支持的文件格式', icon: 'none' })
        return
      }
      selectedFile.value = {
        name: file.name,
        path: file.path,
        size: file.size,
        ext: ext
      }
    },
    fail: (err) => {
      console.log('选择文件失败', err)
    }
  })
}

const removeFile = () => {
  selectedFile.value = null
}

const uploadFileToServer = async (file) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    const uploadTask = uni.uploadFile({
      url: 'http://192.168.74.1:8080/api/rag/ask-with-file',
      filePath: file.path,
      name: 'file',
      formData: {
        question: ''
      },
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        resolve(res)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

const sendMessage = async () => {
  if (!inputText.value.trim() || isLoading.value) return

  const question = inputText.value.trim()
  inputText.value = ''

  if (!currentSessionId.value) {
    await createNewSession()
  }

  const userMsg = {
    role: 'user',
    content: question,
    createdAt: new Date().toLocaleTimeString()
  }
  messages.value.push(userMsg)

  const assistantMsg = {
    role: 'assistant',
    content: '',
    createdAt: new Date().toLocaleTimeString()
  }
  messages.value.push(assistantMsg)

  isLoading.value = true
  scrollTop.value = messages.value.length * 100

  try {
    if (selectedFile.value) {
      const token = uni.getStorageSync('token') || ''
      const decoder = new TextDecoder('utf-8')

      uni.uploadFile({
        url: 'http://192.168.74.1:8080/api/rag/upload',
        filePath: selectedFile.value.path,
        name: 'file',
        header: {
          'Authorization': token ? `Bearer ${token}` : ''
        },
        success: async (res) => {
          try {
            const result = JSON.parse(res.data)
            if (result.code === 200 && result.data && result.data.url) {
              const fileUrl = result.data.url
              await sendMessageWithFileUrl(question, fileUrl)
            } else {
              messages.value[messages.value.length - 1].content = '文件上传失败，请稍后重试~'
              isLoading.value = false
            }
          } catch (e) {
            console.error('解析上传响应失败', e)
            messages.value[messages.value.length - 1].content = '文件上传失败，请稍后重试~'
            isLoading.value = false
          }
        },
        fail: (err) => {
          console.error('上传文件失败:', err)
          messages.value[messages.value.length - 1].content = '抱歉，文件上传失败，请稍后重试~'
          isLoading.value = false
        }
      })
    } else {
      ragAskStream(question, currentSessionId.value, {
        onChunk: (data) => {
          messages.value[messages.value.length - 1].content += data
          scrollTop.value = messages.value.length * 100
        },
        onDone: () => {
          isLoading.value = false
        },
        onError: (err) => {
          console.error('问答失败:', err)
          isLoading.value = false
          if (!messages.value[messages.value.length - 1].content) {
            messages.value[messages.value.length - 1].content = '抱歉，我暂时无法回答这个问题。请换个问题试试~'
          }
        }
      })
    }

  } catch (e) {
    console.error('问答失败:', e)
    messages.value[messages.value.length - 1].content = '抱歉，我暂时无法回答这个问题。请换个问题试试~'
  }

  isLoading.value = false
  scrollTop.value = messages.value.length * 100

  selectedFile.value = null
}

const sendMessageWithFileUrl = async (question, fileUrl) => {
  const token = uni.getStorageSync('token') || ''
  const url = `http://192.168.74.1:8080/api/rag/ask-with-file-url?question=${encodeURIComponent(question)}&sessionId=${currentSessionId.value || ''}`

  let pendingLine = ''

  const processChunks = (text) => {
    pendingLine += text
    while (true) {
      const idx = pendingLine.indexOf('\n')
      if (idx === -1) break
      const line = pendingLine.substring(0, idx).trim()
      pendingLine = pendingLine.substring(idx + 1)
      if (line.startsWith('data:')) {
        const data = line.substring(5).trim().replace(/"/g, '')
        if (data && data !== '[DONE]') {
          messages.value[messages.value.length - 1].content += data
          scrollTop.value = messages.value.length * 100
        }
      }
    }
  }

  uni.request({
    url: url,
    method: 'POST',
    data: { fileUrl: fileUrl },
    header: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    enableChunked: true,
    success: (res) => {
      isLoading.value = false
    },
    fail: (err) => {
      console.error('文件问答请求失败:', err)
      messages.value[messages.value.length - 1].content = '抱歉，文件处理失败，请稍后重试~'
      isLoading.value = false
    }
  })
}

onMounted(() => {
  loadSessionList()
})

onShow(() => {
  loadSessionList()
})
</script>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  background: #F8FAFC;
}

.session-sidebar {
  width: 320rpx;
  background: #fff;
  border-right: 1rpx solid #eee;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 32rpx;
  border-bottom: 1rpx solid #eee;
}

.sidebar-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 24rpx;
}

.new-chat-btn {
  background: #3B82F6;
  color: #fff;
  padding: 16rpx 24rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
  text-align: center;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
  display: flex;
  align-items: center;
}

.session-item.active {
  background: #EEF2FF;
}

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-actions {
  margin-left: 16rpx;
}

.edit-btn {
  font-size: 28rpx;
  padding: 8rpx;
}

.session-title {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 22rpx;
  color: #999;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 24rpx 32rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  display: flex;
  align-items: center;
}

.header-left, .header-right {
  width: 60rpx;
}

.menu-icon, .close-icon, .delete-icon {
  font-size: 40rpx;
}

.chat-title {
  flex: 1;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.chat-list {
  flex: 1;
  padding: 24rpx;
}

.message-item {
  display: flex;
  margin-bottom: 32rpx;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: #E8F5E9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  margin-right: 16rpx;
}

.message-content {
  max-width: 70%;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
}

.message-item.user .message-content {
  background: #3B82F6;
  color: #fff;
  margin-right: 16rpx;
}

.message-text {
  font-size: 28rpx;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
  display: block;
}

.message-item.user .message-time {
  color: rgba(255,255,255,0.7);
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 40rpx;
  text-align: center;
}

.welcome-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
}

.welcome-desc {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 40rpx;
  text-align: center;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
}

.file-info {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.file-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.file-name {
  font-size: 26rpx;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #999;
}

.input-area {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
}

.input-area input {
  flex: 1;
  height: 88rpx;
  background: #F8FAFC;
  border-radius: 44rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
}

.send-btn {
  width: 120rpx;
  height: 88rpx;
  background: #3B82F6;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 16rpx;
  color: #fff;
  font-size: 28rpx;
}

.send-btn.disabled {
  background: #ccc;
}

.upload-btn {
  width: 88rpx;
  height: 88rpx;
  background: #F8FAFC;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  font-size: 40rpx;
}
</style>
