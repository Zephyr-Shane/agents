<template>
  <div class="msg" :class="role">
    <div class="avatar">{{ role === 'assistant' ? '🤖' : '👤' }}</div>
    <div class="bubble">
      <StreamingText :text="content" :streaming="streaming" :speed="20" />
    </div>
  </div>
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
.msg { display: flex; padding: 12px 16px; align-items: flex-start; gap: 10px; }
.msg.user { flex-direction: row-reverse; }
.avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
.bubble {
  max-width: 70%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; color: #333;
  background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.user .bubble { background: #007AFF; color: #fff; }
</style>
