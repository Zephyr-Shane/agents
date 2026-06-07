<template>
  <view class="steps">
    <view v-for="(s, i) in steps" :key="i" class="step" :class="{ active: i === currentStep, done: i < currentStep }">
      <view class="dot">{{ i < currentStep ? '✓' : (i === currentStep && loading ? '...' : i + 1) }}</view>
      <text class="label">{{ s }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  steps: { type: Array, default: () => [] },
  currentStep: { type: Number, default: -1 },
  loading: { type: Boolean, default: false }
})
</script>

<style scoped>
.steps { padding: 24rpx 32rpx; background: #fff; border-radius: 16rpx; margin: 16rpx 32rpx; }
.step { display: flex; align-items: center; padding: 16rpx 0; opacity: 0.5; }
.step.active { opacity: 1; }
.step.done { opacity: 0.8; }
.dot { width: 40rpx; height: 40rpx; border-radius: 50%; background: #E5E5E5; display: flex; align-items: center; justify-content: center; font-size: 22rpx; color: #999; margin-right: 20rpx; flex-shrink: 0; }
.step.active .dot { background: #007AFF; color: #fff; }
.step.done .dot { background: #34C759; color: #fff; }
.label { font-size: 28rpx; color: #333; }
</style>
