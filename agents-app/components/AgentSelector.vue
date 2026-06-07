<template>
  <view class="agent-selector" @tap="showPicker">
    <text class="label">{{ currentLabel }}</text>
    <text class="arrow">▼</text>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  agents: { type: Array, default: () => [] },
  currentAgentId: { type: Number, default: null }
})
const emit = defineEmits(['select'])

const currentLabel = computed(() => {
  if (!props.currentAgentId) return '通用AI对话'
  const a = props.agents.find(x => x.id === props.currentAgentId)
  return a ? a.name : '通用AI对话'
})

function showPicker() {
  const items = ['通用AI对话', ...props.agents.map(a => a.name)]
  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      if (res.tapIndex === 0) emit('select', null)
      else {
        const agent = props.agents[res.tapIndex - 1]
        if (agent) emit('select', agent.id)
      }
    }
  })
}
</script>

<style scoped>
.agent-selector { display: flex; align-items: center; justify-content: center; gap: 8rpx; padding: 12rpx 24rpx; background: #F0F0F0; border-radius: 40rpx; margin: 16rpx auto; max-width: 60%; }
.label { font-size: 26rpx; color: #333; }
.arrow { font-size: 20rpx; color: #999; }
</style>
