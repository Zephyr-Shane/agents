<template>
  <view>
    <page-meta>
      <navigation-bar :title="navTitle" />
    </page-meta>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const navTitle = ref('AI智能体')

onMounted(async () => {
  const auth = useAuthStore()
  auth.init()
  await auth.autoLogin()
})
</script>

<style>
page {
  background-color: #F8F8F8;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  color: #333;
}
</style>
