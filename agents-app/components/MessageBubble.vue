<template>
  <view class="msg" :class="role" v-if="!streaming || content">
    <view class="avatar">{{ role === 'assistant' ? '🤖' : '👤' }}</view>
    <view class="bubble">
      <StreamingText :text="content" :streaming="streaming" :speed="20" />
    </view>
  </view>
</template>

<script setup>
import StreamingText from './StreamingText.vue'
defineProps({
  role: { type: String, default: 'user' },
  content: { type: String, default: '' },
  streaming: { type: Boolean, default: false }
})
</script>

<style scoped>
.msg { display: flex; padding: 20rpx 32rpx; align-items: flex-start; gap: 16rpx; }
.msg.user { flex-direction: row-reverse; }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32rpx; flex-shrink: 0; }
.bubble { max-width: 70%; padding: 20rpx 24rpx; border-radius: 16rpx; font-size: 28rpx; line-height: 1.6; color: #333; background: #fff; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06); }
.user .bubble { background: #007AFF; color: #fff; }
</style>
