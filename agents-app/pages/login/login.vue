<template>
  <view class="login-page">
    <view class="brand">
      <view class="logo">🤖</view>
      <text class="title">AI智能体</text>
      <text class="subtitle">登录以使用全部功能</text>
    </view>

    <view class="form-section">
      <view class="input-group">
        <text class="input-label">用户名</text>
        <input class="form-input" v-model="username" placeholder="请输入用户名" />
      </view>
      <view class="input-group">
        <text class="input-label">密码</text>
        <input class="form-input" v-model="password" placeholder="请输入密码" password="true" />
      </view>

      <view class="login-btn" @tap="handleLogin" :disabled="loading">
        <text class="btn-text">{{ loading ? '登录中...' : '登 录' }}</text>
      </view>

      <view class="agreement">
        <text class="agree-text">登录即表示同意</text>
        <text class="agree-link" @tap="showAgreement">《用户服务协议》</text>
        <text class="agree-text">和</text>
        <text class="agree-link" @tap="showPrivacy">《隐私政策》</text>
      </view>

      <view class="register-link" @tap="goRegister">没有账号？去注册</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const username = ref('admin')
const password = ref('admin')

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  uni.showLoading({ title: '登录中...' })
  try {
    const result = await auth.loginByPassword(username.value.trim(), password.value.trim())
    uni.hideLoading()
    if (result.ok) {
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => uni.switchTab({ url: '/pages/dialogue/dialogue' }), 500)
    } else {
      uni.showToast({ title: result.message, icon: 'none', duration: 3000 })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '登录失败: ' + (e.errMsg || e.message || '网络错误'), icon: 'none', duration: 3000 })
  } finally {
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}

function showAgreement() {
  uni.showModal({
    title: '用户服务协议',
    content: '感谢您使用 AI智能体（以下简称"本应用"）。\n\n'
      + '1. 服务说明\n本应用提供基于人工智能的对话、智能体创建与管理服务。\n\n'
      + '2. 用户责任\n用户应妥善保管账号信息，对账号下的一切行为负责。\n\n'
      + '3. 知识产权\n本应用的所有内容、技术及软件的知识产权归开发者所有。\n\n'
      + '4. 免责声明\n本应用按"现状"提供服务，不保证服务永不中断。\n\n'
      + '5. 协议修改\n我们可能随时修改本协议，修改后的协议一经发布即生效。',
    showCancel: false
  })
}

function showPrivacy() {
  uni.showModal({
    title: '隐私政策',
    content: '我们重视您的隐私。\n\n'
      + '1. 信息收集\n我们收集您注册时提供的用户名、密码、昵称等信息。\n\n'
      + '2. 信息使用\n您的信息仅用于提供账号管理和应用服务。\n\n'
      + '3. 信息保护\n我们采取合理的安全措施保护您的个人信息。\n\n'
      + '4. 第三方服务\n本应用可能集成第三方 AI 服务，数据将按照第三方隐私政策处理。\n\n'
      + '5. 联系我们\n如有疑问，请联系开发者。',
    showCancel: false
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 140rpx 48rpx 80rpx;
  background: linear-gradient(180deg, #FFFFFF 0%, #F0F4F8 100%);
  box-sizing: border-box;
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 72rpx;
}
.logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80rpx;
  margin-bottom: 28rpx;
  box-shadow: 0 12rpx 40rpx rgba(102, 126, 234, 0.35);
}
.title {
  font-size: 44rpx;
  font-weight: 700;
  color: #1A1A1A;
  margin-bottom: 10rpx;
}
.subtitle {
  font-size: 26rpx;
  color: #999;
}

.form-section {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.input-group {
  margin-bottom: 28rpx;
}
.input-label {
  font-size: 26rpx;
  color: #555;
  font-weight: 500;
  margin-bottom: 10rpx;
  display: block;
  padding-left: 8rpx;
}
.form-input {
  height: 92rpx;
  background: #FFFFFF;
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

.login-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.35);
}
.login-btn[disabled] {
  opacity: 0.7;
}
.btn-text {
  font-size: 34rpx;
  font-weight: 600;
  color: #FFFFFF;
  letter-spacing: 12rpx;
}

/* 协议 */
.agreement {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 32rpx;
  gap: 4rpx;
}
.agree-text {
  font-size: 22rpx;
  color: #999;
}
.agree-link {
  font-size: 22rpx;
  color: #667EEA;
}

.register-link {
  text-align: center;
  font-size: 26rpx;
  color: #667EEA;
  margin-top: 28rpx;
}
</style>
