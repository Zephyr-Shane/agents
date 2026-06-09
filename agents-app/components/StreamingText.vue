<template>
  <text class="streaming-text">{{ displayText }}</text>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  streaming: { type: Boolean, default: false },
  speed: { type: Number, default: 30 }
})

const displayText = ref(props.text)
let timer = null
let index = 0

function startTypewriter() {
  if (timer) { clearInterval(timer); timer = null }
  displayText.value = ''
  index = 0
  if (!props.text) return
  timer = setInterval(() => {
    if (index < props.text.length) {
      displayText.value += props.text[index]
      index++
    } else { clearInterval(timer); timer = null }
  }, props.speed)
}

function stopTimer() { if (timer) { clearInterval(timer); timer = null } }

// 流式模式：text 变化时直接追加或全量更新，保持已有内容不重置
watch(() => props.text, (val) => {
  if (props.streaming) {
    // 流式模式下直接显示最新文本（不做 typewriter 重播）
    // 如果新文本比当前显示的长，说明是增量追加
    if (val && val.length > displayText.value.length) {
      displayText.value = val
    } else if (val) {
      // 偶尔发生全量替换的情况，直接设置
      displayText.value = val
    }
  } else {
    // 非流式模式：如果是首次加载或完整内容，做 typewriter 效果
    if (!displayText.value || displayText.value !== val) {
      startTypewriter()
    }
  }
})

watch(() => props.streaming, (val) => {
  if (!val) {
    // 流式结束：如果还没有展示完整内容，开启 typewriter 播放剩余部分
    if (displayText.value !== props.text) {
      startTypewriter()
    } else {
      stopTimer()
    }
  }
})

onUnmounted(stopTimer)
</script>

<style scoped>
.streaming-text { word-break: break-word; white-space: pre-wrap; }
</style>
