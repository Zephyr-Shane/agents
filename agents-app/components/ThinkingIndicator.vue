<template>
  <view class="thinking-msg">
    <view class="avatar">
      <text>🤖</text>
    </view>
    <view class="bubble">
      <view class="dots">
        <view class="dot" v-for="i in 3" :key="i" :style="{ animationDelay: (i - 1) * 0.3 + 's' }" />
      </view>
    </view>
  </view>
</template>

<style scoped>
.thinking-msg { display: flex; padding: 20rpx 32rpx; align-items: flex-start; gap: 16rpx; }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; font-size: 32rpx; flex-shrink: 0; background: #f0f0f0; }
.bubble { padding: 28rpx 32rpx; border-radius: 20rpx; background: #fff; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); border-bottom-left-radius: 4rpx; }
.dots { display: flex; align-items: center; gap: 10rpx; }
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; background: #CCC; animation: dotPulse 1.2s ease-in-out infinite; }
@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}
</style>
