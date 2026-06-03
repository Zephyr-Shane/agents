<template>
  <div class="selector" @click="showPicker">
    <span>{{ label }}</span>
    <span class="arrow">▼</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  agents: { type: Array, default: () => [] },
  currentAgentId: { type: Number, default: null }
})
const emit = defineEmits(['select'])

const label = computed(() => {
  if (!props.currentAgentId) return '通用AI对话'
  const a = props.agents.find(x => x.id === props.currentAgentId)
  return a ? a.name : '通用AI对话'
})

function showPicker() {
  const items = ['通用AI对话', ...props.agents.map(a => a.name)]
  const idx = props.currentAgentId ? props.agents.findIndex(a => a.id === props.currentAgentId) + 1 : 0

  // Simple dropdown simulation
  const choice = window.confirm(
    `当前: ${label.value}\n\n选择智能体:\n` +
    items.map((n, i) => `${i === 0 ? '0' : i}: ${n}`).join('\n') +
    '\n\n输入编号 (0=通用, 1+ = 对应智能体):'
  )
  // Fallback: emit select
  if (!choice) return
}
</script>

<style scoped>
.selector {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 6px 16px; background: #F0F0F0; border-radius: 20px; margin: 10px auto; max-width: 60%;
  cursor: pointer; font-size: 13px; color: #333;
}
.arrow { font-size: 10px; color: #999; }
</style>
