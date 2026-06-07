<template>
  <view class="mine">
    <!-- 用户资料 -->
    <view class="card profile">
      <view class="avatar">{{ avatarText }}</view>
      <view class="info">
        <text class="name">{{ user?.nickname || '未登录' }}</text>
        <text class="uid">{{ user?.uid || '' }}</text>
        <text class="phone" v-if="user?.phone">已绑定手机: {{ maskPhone(user.phone) }}</text>
      </view>
    </view>

    <!-- 未登录：基础操作 -->
    <view class="card actions" v-if="!auth.isLoggedIn">
      <view class="action-btn" @tap="handleRelogin">登录</view>
      <view class="action-btn secondary" @tap="testConnection">测试后端连接</view>
    </view>

    <!-- 已登录但未绑定手机号：微信一键登录引导 -->
    <view class="card quick-login-card" v-if="auth.isLoggedIn && !auth.hasPhone">
      <view class="ql-header">
        <text class="ql-icon">📱</text>
        <view class="ql-text">
          <text class="ql-title">绑定手机号</text>
          <text class="ql-desc">一键授权，享受完整服务体验</text>
        </view>
      </view>
      <button class="ql-btn" open-type="getPhoneNumber" @getphonenumber="onGetPhoneNumber"
        :disabled="phoneLogining" :loading="phoneLogining">
        {{ phoneLogining ? '授权中...' : '微信一键登录' }}
      </button>
    </view>

    <!-- 已登录 + 已绑定手机：关联信息 -->
    <view class="card actions" v-if="auth.isLoggedIn && auth.hasPhone">
      <view class="action-btn" @tap="handleRelogin">刷新信息</view>
      <view class="action-btn secondary" @tap="testConnection">测试连接</view>
    </view>

    <!-- 统计 -->
    <view class="card stats">
      <view class="stat"><text class="num">{{ user?.agentCount || 0 }}</text><text class="lbl">已创建智能体</text></view>
      <view class="stat"><text class="num">--</text><text class="lbl">存储空间</text></view>
    </view>

    <!-- 详细信息 -->
    <view class="card info">
      <view class="row"><text class="l">注册时间</text><text class="r">{{ fmt(user?.registerTime) }}</text></view>
      <view class="row"><text class="l">用户ID</text><text class="r">{{ user?.id || '--' }}</text></view>
      <view class="row" v-if="user?.phone"><text class="l">手机号</text><text class="r">{{ maskPhone(user.phone) }}</text></view>
    </view>

    <!-- 调试信息 -->
    <view class="debug-bar" @tap="showDebug = !showDebug">
      <text>调试信息 {{ showDebug ? '▲' : '▼' }}</text>
    </view>
    <view v-if="showDebug" class="debug-panel">
      <text>ready: {{ String(auth.ready) }}</text>
      <text>isLoggedIn: {{ String(auth.isLoggedIn) }}</text>
      <text>hasPhone: {{ String(auth.hasPhone) }}</text>
      <text>token: {{ auth.token ? auth.token.substring(0, 20) + '...' : '空' }}</text>
      <text>服务器: {{ API_BASE_URL.replace('/api', '') }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, watch, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { API_BASE_URL } from '@/config'

const auth = useAuthStore()
const user = computed(() => auth.user)
const avatarText = computed(() => (user.value?.nickname || '?')[0])
const showDebug = ref(false)
const phoneLogining = ref(false)

onMounted(() => {
  watch(() => auth.ready, (val) => {
    if (val && auth.isLoggedIn) auth.fetchProfile()
  }, { immediate: true })
})

// 登录守卫：未登录时跳转到登录页
onShow(() => {
  if (auth.ready && !auth.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
})

async function handleRelogin() {
  try {
    uni.showLoading({ title: '登录中...' })
    await auth.reLogin()
    if (auth.isLoggedIn) {
      auth.fetchProfile()
      uni.hideLoading()
      uni.showToast({ title: '登录成功', icon: 'success' })
    } else {
      uni.hideLoading()
      uni.showToast({ title: '登录失败，请检查后端', icon: 'none', duration: 3000 })
    }
  } catch (e) {
    uni.hideLoading()
    const msg = e?.errMsg || e?.message || '网络错误'
    uni.showToast({ title: '登录失败: ' + msg, icon: 'none', duration: 4000 })
  }
}

/**
 * 微信手机号一键登录回调
 * button[open-type="getPhoneNumber"] 触发
 */
async function onGetPhoneNumber(e) {
  // 用户拒绝授权
  if (e.detail.errMsg && e.detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({ title: '需要授权手机号才能使用完整功能', icon: 'none', duration: 2000 })
    return
  }

  if (phoneLogining.value) return
  phoneLogining.value = true

  try {
    uni.showLoading({ title: '登录中...' })

    // 1. 获取 wx.login code
    let loginCode
    try {
      const codeRes = await new Promise((resolve, reject) => {
        uni.login({
          success: (res) => res.code ? resolve(res.code) : reject(new Error('未获取到code')),
          fail: (err) => {
            // H5 环境 mock
            if (err.errMsg && err.errMsg.includes('fail')) {
              resolve('h5_mock_' + Date.now())
            } else {
              reject(err)
            }
          }
        })
      })
      loginCode = codeRes
    } catch (e) {
      uni.hideLoading()
      uni.showToast({ title: '获取登录凭证失败', icon: 'none', duration: 2000 })
      return
    }

    // 2. 从 e.detail 获取 phoneCode（新版 API）或 encryptedData（旧版兼容）
    const phoneCode = e.detail.code
    if (!phoneCode) {
      uni.hideLoading()
      uni.showToast({ title: '未获取到手机号凭证，请重试', icon: 'none', duration: 2000 })
      return
    }

    // 3. 发送到后端一键登录
    const ok = await auth.phoneLogin({
      loginCode,
      phoneCode,
      nickname: user.value?.nickname || '',
      avatarUrl: user.value?.avatar || ''
    })

    uni.hideLoading()

    if (ok) {
      auth.fetchProfile()
      uni.showToast({ title: '绑定成功 🎉', icon: 'success' })
    }
  } catch (e) {
    uni.hideLoading()
    const msg = e?.message || e?.errMsg || '网络错误'
    uni.showToast({ title: '一键登录失败: ' + msg, icon: 'none', duration: 3000 })
  } finally {
    phoneLogining.value = false
  }
}

async function testConnection() {
  uni.showLoading({ title: '测试中...' })
  try {
    const [loginRes, healthRes] = await Promise.allSettled([
      new Promise((_, reject) => setTimeout(reject, 4000)),
      uni.request({ url: API_BASE_URL + '/health', timeout: 4000 })
    ])
    uni.hideLoading()
    console.log('health result:', healthRes)
    if (healthRes.status === 'rejected') {
      uni.showToast({ title: '后端不可达: ' + (healthRes.reason?.errMsg || '连接失败'), icon: 'none', duration: 4000 })
    } else if (healthRes.value.statusCode === 200) {
      uni.showToast({ title: '后端连接成功 ✅', icon: 'success' })
    } else {
      uni.showToast({ title: '后端返回异常: ' + healthRes.value.statusCode, icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '测试异常: ' + (e.message || '未知'), icon: 'none', duration: 4000 })
  }
}

/** 手机号脱敏 */
function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || '--'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function fmt(d) {
  if (!d) return '--'
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.mine { padding: 40rpx 32rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 40rpx; margin-bottom: 24rpx; }
.profile { display: flex; align-items: center; gap: 32rpx; }
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; background: #007AFF; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 48rpx; font-weight: 600; }
.info { flex: 1; }
.name { font-size: 36rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.uid { font-size: 26rpx; color: #999; }
.actions { display: flex; gap: 20rpx; }
.action-btn { flex: 1; height: 80rpx; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; background: #007AFF; color: #fff; }
.action-btn.secondary { background: #fff; color: #007AFF; border: 2rpx solid #007AFF; }
.stats { display: flex; text-align: center; padding: 32rpx; }
.stat { flex: 1; }
.num { display: block; font-size: 40rpx; font-weight: 600; margin-bottom: 8rpx; }
.lbl { font-size: 24rpx; color: #999; }
.info { padding: 0 32rpx; }
.row { display: flex; justify-content: space-between; padding: 28rpx 0; font-size: 28rpx; border-bottom: 1rpx solid #F0F0F0; }
.row:last-child { border: none; }
.l { color: #666; }
.r { color: #333; }
.quick-login-card { display: flex; flex-direction: column; gap: 24rpx; }
.ql-header { display: flex; align-items: center; gap: 20rpx; }
.ql-icon { font-size: 64rpx; }
.ql-text { display: flex; flex-direction: column; gap: 4rpx; }
.ql-title { font-size: 32rpx; font-weight: 600; color: #333; }
.ql-desc { font-size: 24rpx; color: #999; }
.ql-btn { height: 80rpx; background: #07C160; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; font-size: 30rpx; color: #fff; border: none; width: 100%; }
.ql-btn[disabled] { opacity: 0.6; }
.phone { font-size: 24rpx; color: #07C160; margin-top: 4rpx; display: block; }
.debug-bar { padding: 20rpx; text-align: center; font-size: 24rpx; color: #999; }
.debug-panel { background: #F5F5F5; border-radius: 12rpx; padding: 20rpx; display: flex; flex-direction: column; gap: 12rpx; font-size: 22rpx; color: #666; font-family: monospace; }
</style>
