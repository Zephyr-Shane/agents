<template>
  <div class="app-container">
    <div class="page-content">
      <router-view />
    </div>
    <div class="tab-bar">
      <div
        class="tab-item"
        :class="{ active: $route.path === '/dialogue' }"
        @click="$router.push('/dialogue')"
      >
        <span class="tab-icon">💬</span>
        <span class="tab-label">对话</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: $route.path === '/mine' }"
        @click="$router.push('/mine')"
      >
        <span class="tab-icon">👤</span>
        <span class="tab-label">我的</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

onMounted(async () => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn) {
    await auth.login('demo_user_' + Date.now())
  }
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { background: #F8F8F8; }
.app-container {
  max-width: 430px;
  margin: 0 auto;
  height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}
.page-content {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 60px;
}
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  height: 56px;
  background: #fff;
  border-top: 1px solid #E5E5E5;
  display: flex;
  align-items: center;
  z-index: 100;
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #999;
  padding: 4px 0;
}
.tab-item.active { color: #007AFF; }
.tab-icon { font-size: 22px; margin-bottom: 2px; }
.tab-label { font-size: 11px; }
</style>
