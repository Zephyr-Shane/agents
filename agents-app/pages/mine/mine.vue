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

    <!-- 已登录：操作按钮 -->
    <view class="card actions" v-if="auth.isLoggedIn">
      <view class="action-btn" @tap="handleRelogin">刷新信息</view>
      <view class="action-btn danger" @tap="handleLogout">退出登录</view>
    </view>

    <!-- 未登录：不应看到此页，登录守卫会跳转 -->
    <view class="card actions" v-else>
      <view class="action-btn" @tap="goLogin">去登录</view>
      <view class="action-btn secondary" @tap="testConnection">测试后端连接</view>
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
      <text>token: {{ auth.token ? auth.token.substring(0, 20) + '...' : '空' }}</text>
      <text>服务器: {{ API_BASE_URL.replace('/api', '') }}</text>
    </view>
  </view>
</template>

<script setup>
import { computed, watch, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { API_BASE_URL } from '@/config'

const auth = useAuthStore()
const user = computed(() => auth.user)
const avatarText = computed(() => (user.value?.nickname || '?')[0])
const showDebug = ref(false)

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

/** 退出登录：清除本地 token，跳回登录页 */
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        auth.logout()
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}

function goLogin() {
  uni.reLaunch({ url: '/pages/login/login' })
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
.action-btn.danger { background: #fff; color: #FF3B30; border: 2rpx solid #FF3B30; }
.stats { display: flex; text-align: center; padding: 32rpx; }
.stat { flex: 1; }
.num { display: block; font-size: 40rpx; font-weight: 600; margin-bottom: 8rpx; }
.lbl { font-size: 24rpx; color: #999; }
.info { padding: 0 32rpx; }
.row { display: flex; justify-content: space-between; padding: 28rpx 0; font-size: 28rpx; border-bottom: 1rpx solid #F0F0F0; }
.row:last-child { border: none; }
.l { color: #666; }
.r { color: #333; }
.phone { font-size: 24rpx; color: #07C160; margin-top: 4rpx; display: block; }
.debug-bar { padding: 20rpx; text-align: center; font-size: 24rpx; color: #999; }
.debug-panel { background: #F5F5F5; border-radius: 12rpx; padding: 20rpx; display: flex; flex-direction: column; gap: 12rpx; font-size: 22rpx; color: #666; font-family: monospace; }
</style>
