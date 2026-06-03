<template>
  <div class="page">
    <div class="card profile">
      <div class="avatar">{{ (user?.nickname || '?')[0] }}</div>
      <div>
        <div class="name">{{ user?.nickname || '未登录' }}</div>
        <div class="uid">{{ user?.uid || '' }}</div>
      </div>
    </div>
    <div class="card stats">
      <div class="stat"><span class="num">{{ user?.agentCount || 0 }}</span><span class="lbl">智能体</span></div>
      <div class="stat"><span class="num">--</span><span class="lbl">存储空间</span></div>
    </div>
    <div class="card info">
      <div class="row"><span class="l">注册时间</span><span class="r">{{ fmt(user?.registerTime) }}</span></div>
      <div class="row"><span class="l">用户ID</span><span class="r">{{ user?.id || '--' }}</span></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const user = computed(() => auth.user)

onMounted(() => { if (auth.isLoggedIn) auth.fetchProfile() })

function fmt(d) {
  if (!d) return '--'
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
}
</script>

<style scoped>
.page { padding: 20px 16px; }
.card { background:#fff; border-radius:12px; padding:20px; margin-bottom:12px; }
.profile { display:flex; align-items:center; gap:16px; }
.avatar { width:48px; height:48px; border-radius:50%; background:#007AFF; color:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:600; }
.name { font-size:16px; font-weight:600; }
.uid { font-size:12px; color:#999; margin-top:4px; }
.stats { display:flex; text-align:center; }
.stat { flex:1; }
.num { display:block; font-size:20px; font-weight:600; margin-bottom:4px; }
.lbl { font-size:12px; color:#999; }
.info { padding:0 20px; }
.row { display:flex; justify-content:space-between; padding:14px 0; font-size:14px; border-bottom:1px solid #F0F0F0; }
.row:last-child { border:none; }
.l { color:#666; }
.r { color:#333; }
</style>
