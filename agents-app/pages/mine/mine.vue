<template>
  <view class="mine">
    <!-- 用户资料卡片 -->
    <view class="profile-card">
      <view class="avatar-wrap" @tap="changeAvatar">
        <image v-if="user?.avatar" class="avatar-img" :src="user.avatar" mode="aspectFill" />
        <view v-else class="avatar-text">{{ avatarText }}</view>
      </view>
      <view class="profile-info" @tap="editNickname">
        <text class="profile-name">{{ user?.nickname || '未登录' }}</text>
        <text class="profile-uid">{{ user?.uid || '' }}</text>
        <text v-if="user?.phone" class="profile-phone">📱 {{ maskPhone(user.phone) }}</text>
      </view>
    </view>

    <!-- 统计数据 -->
    <view class="stats-row">
      <view class="stat-item">
        <text class="stat-num">{{ user?.agentCount || 0 }}</text>
        <text class="stat-lbl">智能体</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-num">{{ chatStore.conversations.length }}</text>
        <text class="stat-lbl">对话</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-num">--</text>
        <text class="stat-lbl">存储</text>
      </view>
    </view>

    <!-- 操作列表 -->
    <view class="action-list">
      <view class="action-item" @tap="handleLogout">
        <text class="action-icon danger-icon">🚪</text>
        <text class="action-text danger-text">退出登录</text>
        <text class="action-arrow">›</text>
      </view>
    </view>

    <!-- 信息卡片 -->
    <view class="info-card">
      <view class="info-row">
        <text class="info-label">注册时间</text>
        <text class="info-value">{{ fmt(user?.registerTime) }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">用户ID</text>
        <text class="info-value">{{ user?.id || '--' }}</text>
      </view>
      <view v-if="user?.phone" class="info-row">
        <text class="info-label">绑定手机</text>
        <text class="info-value">{{ maskPhone(user.phone) }}</text>
      </view>
    </view>

    <!-- 版本信息 -->
    <text class="version">AI 智能体 v1.0.0</text>
  </view>
</template>

<script setup>
import { computed, watch, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { uploadAvatarApi, updateProfileApi } from '@/api/auth'

const auth = useAuthStore()
const chatStore = useChatStore()
const user = computed(() => auth.user)
const avatarText = computed(() => (user.value?.nickname || '?')[0])

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
  // 刷新信息
  if (auth.ready && auth.isLoggedIn) {
    auth.fetchProfile()
    chatStore.loadGeneralConversations()
  }
})

/** 退出登录 */
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

/** 更换头像 */
function changeAvatar() {
  uni.showActionSheet({
    itemList: ['选择微信头像', '从相册选择'],
    success: (res) => {
      if (res.tapIndex === 0) {
        chooseWeChatAvatar()
      } else if (res.tapIndex === 1) {
        chooseFromAlbum()
      }
    }
  })
}

/** 从微信头像选择 (wx.chooseAvatar) */
function chooseWeChatAvatar() {
  // wx.chooseAvatar 是微信小程序特有 API
  if (typeof wx === 'undefined' || !wx.chooseAvatar) {
    uni.showToast({ title: '当前环境不支持', icon: 'none' })
    return
  }
  wx.chooseAvatar({
    success: (res) => {
      uploadAvatar(res.avatarUrl)
    },
    fail: () => {
      uni.showToast({ title: '取消选择', icon: 'none' })
    }
  })
}

/** 从相册选择 */
function chooseFromAlbum() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      if (res.tempFilePaths && res.tempFilePaths.length > 0) {
        uploadAvatar(res.tempFilePaths[0])
      }
    },
    fail: () => {
      uni.showToast({ title: '取消选择', icon: 'none' })
    }
  })
}

/** 上传头像文件并更新用户资料 */
async function uploadAvatar(filePath) {
  uni.showLoading({ title: '上传中...' })
  try {
    const res = await uploadAvatarApi(filePath)
    uni.hideLoading()
    if (res.code === 0 && res.data) {
      await auth.updateProfile(null, res.data)
      uni.showToast({ title: '头像更新成功', icon: 'success' })
    } else {
      uni.showToast({ title: res.message || '上传失败', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '上传失败: ' + (e.message || '网络错误'), icon: 'none' })
  }
}

/** 编辑昵称 */
function editNickname() {
  const current = user.value?.nickname || ''
  uni.showModal({
    title: '修改昵称',
    editable: true,
    placeholderText: '请输入昵称',
    content: current,
    success: async (res) => {
      if (res.confirm && res.content && res.content.trim()) {
        const nickname = res.content.trim()
        uni.showLoading({ title: '保存中...' })
        try {
          const ok = await auth.updateProfile(nickname, null)
          uni.hideLoading()
          if (ok) {
            uni.showToast({ title: '昵称已更新', icon: 'success' })
          } else {
            uni.showToast({ title: '保存失败', icon: 'none' })
          }
        } catch (e) {
          uni.hideLoading()
          uni.showToast({ title: '保存失败: ' + (e.message || '网络错误'), icon: 'none' })
        }
      }
    }
  })
}

/** 手机号脱敏 */
function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || '--'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function fmt(d) {
  if (!d) return '--'
  const date = new Date(typeof d === 'string' ? d.replace(' ', 'T') : d)
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.mine {
  padding: 24rpx;
  padding-bottom: 100rpx;
  background: #F5F7FA;
  min-height: 100vh;
}

/* ===== 用户资料卡片 ===== */
.profile-card {
  display: flex;
  align-items: center;
  gap: 28rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 36rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.avatar-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
}
.avatar-text {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #fff;
  font-weight: 600;
}
.profile-info {
  flex: 1;
}
.profile-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #1A1A1A;
  display: block;
  margin-bottom: 6rpx;
}
.profile-uid {
  font-size: 24rpx;
  color: #BBB;
  display: block;
}
.profile-phone {
  font-size: 24rpx;
  color: #667EEA;
  display: block;
  margin-top: 4rpx;
}

/* ===== 统计行 ===== */
.stats-row {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 40rpx;
  font-weight: 700;
  color: #1A1A1A;
  margin-bottom: 6rpx;
}
.stat-lbl {
  font-size: 22rpx;
  color: #999;
}
.stat-divider {
  width: 2rpx;
  height: 48rpx;
  background: #F0F0F0;
}

/* ===== 操作列表 ===== */
.action-list {
  background: #fff;
  border-radius: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
  overflow: hidden;
}
.action-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
}
.action-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
  width: 40rpx;
  text-align: center;
}
.danger-icon {
  font-size: 36rpx;
}
.action-text {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}
.danger-text {
  color: #FF3B30;
}
.action-arrow {
  font-size: 32rpx;
  color: #CCC;
}

/* ===== 信息卡片 ===== */
.info-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 0 32rpx;
  margin-bottom: 32rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 2rpx solid #F8F8F8;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  font-size: 26rpx;
  color: #999;
}
.info-value {
  font-size: 26rpx;
  color: #333;
}

/* ===== 版本号 ===== */
.version {
  display: block;
  text-align: center;
  font-size: 22rpx;
  color: #CCC;
  padding: 20rpx;
}
</style>
