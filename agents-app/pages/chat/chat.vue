<template>
  <view class="chat-page">
    <!-- 消息区域 -->
    <scroll-view class="msg-area" scroll-y :scroll-into-view="scrollId" :scroll-with-animation="true">
      <view v-if="!chatStore.messages.length" class="empty">
        <text style="font-size:64rpx">💬</text>
        <text style="font-size:26rpx;color:#999;margin-top:12rpx">开始新的对话</text>
      </view>
      <MessageBubble v-for="m in chatStore.messages" :key="m.id" :role="m.role" :content="m.content" :avatar="m.role === 'assistant' ? '' : userAvatar" />
      <MessageBubble v-if="chatStore.streaming" role="assistant" :content="chatStore.streamingContent" :streaming="true" avatar="" />
      <view :id="'bottom-' + scrollTick" />
    </scroll-view>

    <!-- 输入区 -->
    <view class="input-row">
      <input class="input" v-model="msg" placeholder="输入消息..." :disabled="chatStore.streaming" @confirm="send" confirm-type="send" />
      <view class="send-btn" :class="{ disabled: !msg.trim() || chatStore.streaming }" @tap="send">发送</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import MessageBubble from '@/components/MessageBubble.vue'

const chatStore = useChatStore()
const authStore = useAuthStore()

const userAvatar = computed(() => authStore.user?.avatar || '')

const msg = ref('')
const scrollId = ref('')
const scrollTick = ref(0)
const sending = ref(false)
const pageConvId = ref(null)

onLoad(async (query) => {
  pageConvId.value = query.conversationId || null
  if (pageConvId.value) {
    await chatStore.switchConversation(pageConvId.value)
  }
  scrollToBottom()
})

// 每次进入页面时重新从 API 拉取最新消息
onShow(() => {
  if (pageConvId.value && !chatStore.streaming) {
    chatStore.refreshMessages()
  }
})

// 新消息到达时自动滚动到底部
watch(() => chatStore.messages.length, () => {
  nextTick(scrollToBottom)
})
watch(() => chatStore.streamingContent, () => {
  if (chatStore.streaming) nextTick(scrollToBottom)
})

function scrollToBottom() {
  scrollTick.value++
  scrollId.value = 'bottom-' + scrollTick.value
}

function send() {
  const text = msg.value.trim()
  if (!text || chatStore.streaming || sending.value) return
  sending.value = true
  msg.value = ''
  chatStore.sendMessage(null, text, []).finally(() => {
    sending.value = false
  })
}
</script>

<style scoped>
.chat-page {
  height: 100vh;
  background: #F5F7FA;
}

/* 消息区域 */
.msg-area {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 120rpx;
  overflow-y: auto;
  padding: 0 24rpx;
  padding-bottom: 20rpx;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

/* 输入区 */
.input-row {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom, 0rpx));
  background: #fff;
  border-top: 1rpx solid #F0F0F0;
  gap: 16rpx;
  z-index: 10;
}
.input {
  flex: 1;
  height: 80rpx;
  background: #F5F7FA;
  border-radius: 40rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
}
.send-btn {
  width: 120rpx;
  height: 80rpx;
  background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%);
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28rpx;
  font-weight: 500;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(102, 126, 234, 0.3);
}
.send-btn.disabled {
  opacity: 0.5;
}
</style>
