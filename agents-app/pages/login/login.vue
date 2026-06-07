<template>
  <view class="login-page">
    <!-- 顶部品牌区 -->
    <view class="brand">
      <view class="logo">🤖</view>
      <text class="title">AI智能体</text>
      <text class="subtitle">让你的智能助理随叫随到</text>
    </view>

    <!-- 登录按钮区 -->
    <view class="login-section">
      <view class="feature-list">
        <view class="feature-item">
          <text class="fi-icon">💬</text>
          <text class="fi-text">智能对话，随时提问</text>
        </view>
        <view class="feature-item">
          <text class="fi-icon">🧠</text>
          <text class="fi-text">自然语言创建专属智能体</text>
        </view>
        <view class="feature-item">
          <text class="fi-icon">📎</text>
          <text class="fi-text">上传文件，AI 辅助分析</text>
        </view>
      </view>

      <button class="wechat-login-btn"
        open-type="getPhoneNumber"
        @getphonenumber="onGetPhoneNumber"
        :disabled="loading"
        :loading="loading">
        <text class="btn-icon">📱</text>
        <text class="btn-text">{{ loading ? '登录中...' : '微信一键登录' }}</text>
      </button>

      <view class="agreement">
        <text class="agree-text">登录即表示同意</text>
        <text class="agree-link" @tap="showAgreement">《用户服务协议》</text>
        <text class="agree-text">和</text>
        <text class="agree-link" @tap="showPrivacy">《隐私政策》</text>
      </view>
    </view>

    <!-- 调试/备用：开发者登录入口 -->
    <view class="dev-login" v-if="showDevLogin" @tap.stop>
      <view class="dev-title">开发者登录</view>
      <input class="dev-input" v-model="devCode" placeholder="输入测试 code" />
      <view class="dev-btn" @tap="handleDevLogin">开发者登录</view>
    </view>
    <view class="dev-toggle" @tap="showDevLogin = !showDevLogin">
      <text>{{ showDevLogin ? '收起' : '开发者选项' }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const showDevLogin = ref(false)
const devCode = ref('')

// 页面每次显示时检查：如已登录则跳过
onShow(() => {
  if (auth.ready && auth.isLoggedIn) {
    redirectToHome()
  }
})

// 等待 auth.ready 后再检查一次（处理首次加载时 autoLogin 尚未完成的情况）
watch(() => auth.ready, (ready) => {
  if (ready && auth.isLoggedIn) {
    redirectToHome()
  }
})

/**
 * 微信手机号一键登录回调
 */
async function onGetPhoneNumber(e) {
  // 用户拒绝授权
  if (e.detail.errMsg && e.detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({ title: '需要授权手机号才能使用', icon: 'none', duration: 2000 })
    return
  }

  if (loading.value) return
  loading.value = true

  try {
    // 1. 获取 wx.login code
    const loginCode = await getLoginCode()
    if (!loginCode) {
      return
    }

    // 2. 从 e.detail 获取 phoneCode
    const phoneCode = e.detail.code
    if (!phoneCode) {
      uni.showToast({ title: '未获取到手机号凭证，请重试', icon: 'none', duration: 2000 })
      return
    }

    uni.showLoading({ title: '登录中...' })

    // 3. 发送到后端一键登录
    const ok = await auth.phoneLogin({
      loginCode,
      phoneCode,
      nickname: '',
      avatarUrl: ''
    })

    uni.hideLoading()

    if (ok) {
      uni.showToast({ title: '欢迎回来 🎉', icon: 'success' })
      // 跳转到首页
      setTimeout(() => redirectToHome(), 500)
    }
  } catch (e) {
    uni.hideLoading()
    const msg = e?.message || e?.errMsg || '网络错误'
    uni.showToast({ title: '登录失败: ' + msg, icon: 'none', duration: 3000 })
  } finally {
    loading.value = false
  }
}

/**
 * 获取 wx.login code（支持 H5 mock）
 */
function getLoginCode() {
  return new Promise((resolve) => {
    uni.login({
      success: (res) => {
        if (res.code) resolve(res.code)
        else resolve(null)
      },
      fail: (err) => {
        // H5 环境 mock
        if (err.errMsg && err.errMsg.includes('fail')) {
          resolve('h5_mock_' + Date.now())
        } else {
          resolve(null)
        }
      }
    })
  })
}

/**
 * 开发者登录（绕过手机号授权）
 */
async function handleDevLogin() {
  if (!devCode.value.trim()) {
    uni.showToast({ title: '请输入测试 code', icon: 'none' })
    return
  }
  loading.value = true
  uni.showLoading({ title: '登录中...' })
  try {
    await auth.reLogin()
    uni.hideLoading()
    if (auth.isLoggedIn) {
      uni.showToast({ title: '开发者登录成功', icon: 'success' })
      setTimeout(() => redirectToHome(), 500)
    } else {
      uni.showToast({ title: '登录失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '登录失败: ' + (e.message || '未知'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

/** 跳转到首页（对话 tab） */
function redirectToHome() {
  uni.switchTab({ url: '/pages/dialogue/dialogue' })
}

function showAgreement() {
  uni.showModal({
    title: '用户服务协议',
    content: 'AI智能体是一款智能对话助手应用...（协议内容待完善）',
    showCancel: false
  })
}

function showPrivacy() {
  uni.showModal({
    title: '隐私政策',
    content: '我们重视您的隐私。我们仅收集必要的账号信息以提供服务...（隐私政策待完善）',
    showCancel: false
  })
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  min-height: 100vh;
  padding: 120rpx 48rpx 60rpx;
  background: linear-gradient(180deg, #FFFFFF 0%, #F5F7FA 100%);
  box-sizing: border-box;
}

/* ===== 品牌区 ===== */
.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.logo {
  width: 160rpx;
  height: 160rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #007AFF, #5856D6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 122, 255, 0.3);
}
.title {
  font-size: 44rpx;
  font-weight: 700;
  color: #1A1A1A;
  margin-bottom: 12rpx;
}
.subtitle {
  font-size: 28rpx;
  color: #999;
}

/* ===== 登录区 ===== */
.login-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.feature-list {
  width: 100%;
  margin-bottom: 48rpx;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 0;
}
.fi-icon {
  font-size: 40rpx;
  width: 56rpx;
  text-align: center;
}
.fi-text {
  font-size: 28rpx;
  color: #555;
}

/* 微信登录按钮 */
.wechat-login-btn {
  width: 100%;
  height: 96rpx;
  background: #07C160;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border: none;
  margin: 0;
  box-shadow: 0 4rpx 16rpx rgba(7, 193, 96, 0.3);
}
.wechat-login-btn::after {
  border: none;
}
.wechat-login-btn[disabled] {
  opacity: 0.7;
}
.btn-icon {
  font-size: 36rpx;
}
.btn-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
}

/* 协议 */
.agreement {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 28rpx;
  gap: 4rpx;
}
.agree-text {
  font-size: 22rpx;
  color: #999;
}
.agree-link {
  font-size: 22rpx;
  color: #07C160;
}

/* ===== 开发者选项 ===== */
.dev-login {
  width: 100%;
  padding: 32rpx;
  background: #fff;
  border-radius: 20rpx;
  margin-top: 24rpx;
}
.dev-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #666;
  margin-bottom: 16rpx;
}
.dev-input {
  height: 64rpx;
  background: #F5F5F5;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  margin-bottom: 16rpx;
}
.dev-btn {
  height: 64rpx;
  background: #007AFF;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #fff;
}
.dev-toggle {
  padding: 20rpx;
  font-size: 22rpx;
  color: #CCC;
}
</style>
