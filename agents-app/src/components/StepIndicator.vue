<template>
  <div class="steps">
    <div v-for="(s, i) in steps" :key="i" class="step" :class="{ active: i === currentStep, done: i < currentStep }">
      <span class="dot">{{ i < currentStep ? '✓' : (i === currentStep && loading ? '...' : i + 1) }}</span>
      <span class="label">{{ s }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  steps: { type: Array, default: () => [] },
  currentStep: { type: Number, default: -1 },
  loading: { type: Boolean, default: false }
})
</script>

<style scoped>
.steps { padding: 16px 20px; background: #fff; border-radius: 12px; margin: 12px 16px; }
.step { display: flex; align-items: center; padding: 10px 0; opacity: 0.5; }
.step.active { opacity: 1; }
.step.done { opacity: 0.8; }
.dot {
  width: 24px; height: 24px; border-radius: 50%; background: #E5E5E5; display: flex;
  align-items: center; justify-content: center; font-size: 12px; color: #999; margin-right: 12px; flex-shrink: 0;
}
.step.active .dot { background: #007AFF; color: #fff; }
.step.done .dot { background: #34C759; color: #fff; }
.label { font-size: 14px; color: #333; }
</style>
