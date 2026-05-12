<template>
  <div id="app">
    <Page />
  </div>
</template>

<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/auth/login' })
    } else {
      // 获取并保存客户端推送ID
      this.savePushClientId()
    }
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    savePushClientId() {
      // 获取 uni-push 客户端 ID
      uni.getPushClientId({
        success: (res) => {
          if (res.cid) {
            console.log('获取到客户端ID:', res.cid)
            uni.request({
              url: 'http://192.168.74.1:8080/api/user/client-id',
              method: 'POST',
              header: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + uni.getStorageSync('token')
              },
              data: { clientId: res.cid },
              success: (res) => {
                console.log('客户端ID保存结果:', res.data)
              },
              fail: (err) => {
                console.log('保存客户端ID失败:', err)
              }
            })
          }
        },
        fail: (err) => {
          console.log('获取客户端ID失败:', err)
        }
      })
    }
  }
}
</script>

<style>
page {
  background-color: #f5f5f5;
}
</style>