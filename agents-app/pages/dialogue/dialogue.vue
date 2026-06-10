<template>
  <view class="dialogue">
    <!-- 搜索栏 -->
    <view class="search-box">
      <text class="search-icon">🔍</text>
      <input class="search-input" v-model="searchTerm" placeholder="搜索会话..." confirm-type="search" />
      <text class="search-clear" v-if="searchTerm" @tap="searchTerm = ''">✕</text>
    </view>

    <!-- 会话列表 -->
    <scroll-view class="conv-list" scroll-y :show-scrollbar="true">
      <view class="conv-count-label">会话 ({{ filteredConversations.length }})</view>

      <view v-for="c in filteredConversations" :key="c.id"
        class="conv-item" @tap="openConversation(c.id)">
        <view class="conv-icon">{{ c.title ? c.title[0] : '💬' }}</view>
        <view class="conv-body">
          <view class="conv-top">
            <text class="conv-name">{{ c.title }}</text>
            <text class="conv-time">{{ formatTime(c.lastMessageTime || c.createTime) }}</text>
          </view>
          <text class="conv-preview">{{ c.lastMessage || '暂无消息' }}</text>
        </view>
      </view>

      <view v-if="filteredConversations.length === 0" class="conv-empty">
        <text style="font-size:80rpx;opacity:0.4">💬</text>
        <text style="font-size:28rpx;color:#999;margin-top:16rpx">
          {{ searchTerm ? '未找到匹配的会话' : '暂无会话，开始新对话吧' }}
        </text>
      </view>
    </scroll-view>

    <!-- 新对话悬浮按钮 -->
    <view class="fab-btn" @tap="newConv">
      <text class="fab-icon">＋</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()
const chatStore = useChatStore()
const agentStore = useAgentStore()

const searchTerm = ref('')

// 模糊搜索过滤
const filteredConversations = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  if (!term) return chatStore.conversations
  return chatStore.conversations.filter(c =>
    c.title && c.title.toLowerCase().includes(term)
  )
})

onMounted(() => {
  watch(() => authStore.ready, (val) => {
    if (val && authStore.isLoggedIn) {
      agentStore.loadAgents()
      loadConvs()
    }
  }, { immediate: true })
})

onShow(() => {
  if (authStore.ready && !authStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  if (authStore.ready && authStore.isLoggedIn) {
    agentStore.loadAgents()
    loadConvs()
  }
})

function loadConvs() {
  if (agentStore.currentAgentId) {
    chatStore.loadConversations(agentStore.currentAgentId)
  } else {
    chatStore.loadAllConversations()
  }
}

/** 打开已有会话 */
function openConversation(convId) {
  chatStore.currentConvId = convId
  uni.navigateTo({
    url: '/pages/chat/chat?conversationId=' + convId
  })
}

/** 新对话 */
async function newConv() {
  await chatStore.startNewConversation(agentStore.currentAgentId)
  // startNewConversation 设置了 currentConvId
  uni.navigateTo({
    url: '/pages/chat/chat?conversationId=' + chatStore.currentConvId
  })
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(typeof t === 'string' ? t.replace(' ', 'T') : t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000 && d.getDate() === now.getDate()) {
    return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.dialogue {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F5F7FA;
}

/* 搜索栏 */
.search-box {
  display: flex;
  align-items: center;
  margin: 16rpx 24rpx;
  padding: 0 28rpx;
  height: 72rpx;
  background: #fff;
  border-radius: 36rpx;
  gap: 12rpx;
  flex-shrink: 0;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.search-icon { font-size: 28rpx; flex-shrink: 0; }
.search-input { flex: 1; font-size: 28rpx; height: 100%; background: transparent; }
.search-clear { font-size: 28rpx; color: #999; padding: 8rpx; flex-shrink: 0; }

/* 会话列表 */
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 0 160rpx;
  background: #fff;
  border-radius: 20rpx;
  margin: 0 24rpx;
}
.conv-count-label {
  font-size: 24rpx;
  color: #AAA;
  padding: 20rpx 24rpx 12rpx;
  line-height: 32rpx;
  height: 64rpx;
  box-sizing: border-box;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  height: 128rpx;
  min-height: 128rpx;
  box-sizing: border-box;
  background: #fff;
  margin-bottom: 0;
  border-bottom: 2rpx solid #F5F5F5;
}
.conv-item:last-child { border-bottom: none; }
.conv-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #fff;
  flex-shrink: 0;
}
.conv-body {
  flex: 1;
  min-width: 0;
  height: 88rpx;
}
.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4rpx;
  line-height: 40rpx;
}
.conv-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A1A;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
  line-height: 40rpx;
  height: 40rpx;
}
.conv-time {
  font-size: 22rpx;
  color: #BBB;
  flex-shrink: 0;
  line-height: 40rpx;
  height: 40rpx;
}
.conv-preview {
  font-size: 24rpx;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  line-height: 36rpx;
  height: 36rpx;
}
.conv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
}

/* FAB 悬浮按钮 */
.fab-btn {
  position: fixed;
  bottom: 140rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(102, 126, 234, 0.4);
  z-index: 999;
}
.fab-icon {
  font-size: 56rpx;
  color: #FFFFFF;
  font-weight: 300;
  line-height: 1;
}
</style>
