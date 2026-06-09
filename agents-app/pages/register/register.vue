<template>
  <view class="register-page">
    <view class="brand">
      <view class="logo">🤖</view>
      <text class="title">注册账号</text>
      <text class="subtitle">创建一个新的账号</text>
    </view>

    <view class="form-section">
      <input class="form-input" v-model="username" placeholder="用户名" />
      <input class="form-input" v-model="nickname" placeholder="昵称" />
      <input class="form-input" v-model="password" placeholder="密码" password="true" />
      <input class="form-input" v-model="confirmPassword" placeholder="确认密码" password="true" />
      <view class="error-text" v-if="errorMsg">{{ errorMsg }}</view>
      <view class="register-btn" @tap="handleRegister" :disabled="loading">
        <text class="btn-text">{{ loading ? '注册中...' : '注 册' }}</text>
      </view>
      <view class="login-link" @tap="goLogin">已有账号？去登录</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''
  if (!username.value.trim()) { errorMsg.value = '请输入用户名'; return }
  if (!nickname.value.trim()) { errorMsg.value = '请输入昵称'; return }
  if (!password.value.trim()) { errorMsg.value = '请输入密码'; return }
  if (password.value !== confirmPassword.value) { errorMsg.value = '两次密码输入不一致'; return }
  if (password.value.length < 4) { errorMsg.value = '密码至少4位'; return }

  loading.value = true
  uni.showLoading({ title: '注册中...' })
  try {
    const result = await auth.register(
      username.value.trim(),
      password.value.trim(),
      nickname.value.trim()
    )
    uni.hideLoading()
    if (result.ok) {
      uni.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => uni.redirectTo({ url: '/pages/mine/mine' }), 500)
    } else {
      errorMsg.value = result.message
      uni.showToast({ title: result.message, icon: 'none', duration: 3000 })
    }
  } catch (e) {
    uni.hideLoading()
    const msg = e?.errMsg || e?.message || '网络错误'
    errorMsg.value = msg
    uni.showToast({ title: '注册失败: ' + msg, icon: 'none', duration: 3000 })
  } finally {
    loading.value = false
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style scoped>
.register-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 100rpx 48rpx 60rpx;
  background: linear-gradient(180deg, #FFFFFF 0%, #F5F7FA 100%);
  box-sizing: border-box;
}
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
}
.logo {
  width: 120rpx;
  height: 120rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.3);
}
.title {
  font-size: 40rpx;
  font-weight: 700;
  color: #1A1A1A;
  margin-bottom: 8rpx;
}
.subtitle {
  font-size: 26rpx;
  color: #999;
}
.form-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.form-input {
  height: 92rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
  border: 2rpx solid #E8ECF0;
  box-sizing: border-box;
  width: 100%;
  transition: border-color 0.2s;
}
.form-input:focus {
  border-color: #667EEA;
}
.error-text {
  font-size: 24rpx;
  color: #FF3B30;
  text-align: center;
}
.register-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8rpx;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.35);
}
.register-btn[disabled] {
  opacity: 0.7;
}
.btn-text {
  font-size: 34rpx;
  font-weight: 600;
  color: #FFFFFF;
  letter-spacing: 8rpx;
}
.login-link {
  text-align: center;
  font-size: 26rpx;
  color: #667EEA;
  margin-top: 8rpx;
}
</style>
