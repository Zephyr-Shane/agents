<template>
  <view class="msg" :class="role" v-if="!streaming || content">
    <view class="avatar">
      <image v-if="avatar" :src="avatar" class="avatar-img" mode="aspectFill" />
      <text v-else>{{ role === 'assistant' ? '🤖' : '👤' }}</text>
    </view>
    <view class="bubble" v-if="role === 'user'">{{ content }}</view>
    <view class="bubble" v-else>
      <rich-text :nodes="html" />
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { mdToHtml } from '@/utils/markdown'

const props = defineProps({
  role: { type: String, default: 'user' },
  content: { type: String, default: '' },
  streaming: { type: Boolean, default: false },
  avatar: { type: String, default: '' }
})

const html = computed(() => mdToHtml(props.content))
</script>

<style scoped>
.msg { display: flex; padding: 20rpx 32rpx; align-items: flex-start; gap: 16rpx; }
.msg.user { flex-direction: row-reverse; }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; font-size: 32rpx; flex-shrink: 0; background: #f0f0f0; }
.avatar-img { width: 100%; height: 100%; }
.bubble { max-width: 70%; padding: 24rpx 28rpx; border-radius: 20rpx; font-size: 28rpx; line-height: 1.7; color: #1A1A1A; background: #fff; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); overflow-wrap: break-word; word-break: break-word; }
.user .bubble { background: linear-gradient(135deg, #667EEA 0%, #764BA2 100%); color: #fff; border-bottom-right-radius: 4rpx; }
.assistant .bubble { border-bottom-left-radius: 4rpx; }
/* rich-text 内 a 标签在深色气泡中的颜色 */
.user .bubble a { color: #fff; }
</style>
