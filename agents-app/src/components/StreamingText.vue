<template>
  <span class="streaming-text">{{ displayText }}<span v-if="streaming" class="cursor">|</span></span>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  streaming: { type: Boolean, default: false },
  speed: { type: Number, default: 30 }
})

const displayText = ref('')
let timer = null
let index = 0

function startStreaming() {
  stopStreaming()
  displayText.value = ''
  index = 0
  if (!props.text) return
  timer = setInterval(() => {
    if (index < props.text.length) {
      displayText.value += props.text[index]
      index++
    } else stopStreaming()
  }, props.speed)
}

function stopStreaming() {
  if (timer) { clearInterval(timer); timer = null }
}

watch(() => props.text, (val) => {
  if (props.streaming) startStreaming()
  else displayText.value = val
})

watch(() => props.streaming, (val) => {
  if (!val) { displayText.value = props.text; stopStreaming() }
})

onUnmounted(stopStreaming)
</script>

<style scoped>
.streaming-text { word-break: break-word; white-space: pre-wrap; }
.cursor { animation: blink 1s steps(1) infinite; color: #007AFF; }
@keyframes blink { 50% { opacity: 0; } }
</style>
