<template>
  <view class="create-page">
    <view class="create-header">
      <text class="create-title">用自然语言描述你想要的智能体</text>
      <text class="create-desc">例如：「创建一个PDF文档知识库助手，只能读取上传文件回答，禁止联网」</text>
    </view>

    <view class="create-input-area">
      <textarea class="create-input" v-model="desc" placeholder="描述智能体的功能、风格、约束..." :disabled="creating" auto-height />
    </view>

    <view class="create-info">
      <text class="info-title">✨ 你可以这样描述</text>
      <view class="info-item" @tap="desc = '帮我创建一个英语学习助手，专门用于日常对话练习，要求语气友好耐心，能纠正语法错误'">
        <text class="info-tag">英语学习助手</text>
      </view>
      <view class="info-item" @tap="desc = '创建一个代码审查专家，能够分析上传的代码文件，指出潜在bug和性能问题，给出优化建议'">
        <text class="info-tag">代码审查专家</text>
      </view>
      <view class="info-item" @tap="desc = '创建一个旅游规划助手，可以联网搜索最新的旅游信息，帮助制定行程计划'">
        <text class="info-tag">旅游规划助手</text>
      </view>
    </view>

    <view class="create-btn" :class="{ disabled: !desc.trim() || creating }" @tap="doCreate">
      {{ creating ? '创建中...' : '开始创建智能体' }}
    </view>

    <StepIndicator v-if="creating" :steps="steps" :currentStep="step" :loading="creating" />

    <!-- 成功弹窗 -->
    <view v-if="showSuccess" class="modal" @tap="showSuccess=false">
      <view class="modal-box" @tap.stop>
        <text style="font-size:64rpx">🎉</text>
        <text class="modal-title">创建完成</text>
        <text class="modal-desc">智能体「{{ createdName }}」已创建成功！</text>
        <view class="modal-actions">
          <view class="mbtn" @tap="goChat">去对话</view>
          <view class="mbtn primary" @tap="createAgain">继续创建</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { createAgentFromNL } from '@/api/agent'
import StepIndicator from '@/components/StepIndicator.vue'

const desc = ref('')
const creating = ref(false)
const step = ref(-1)
const showSuccess = ref(false)
const createdName = ref('')
const steps = ['正在解析角色定位', '正在配置知识库能力', '正在配置联网权限', '智能体创建完成']

async function doCreate() {
  if (!desc.value.trim() || creating.value) return
  creating.value = true
  step.value = 0
  try {
    const res = await createAgentFromNL(desc.value.trim())
    if (res && res.code === 0 && res.data) {
      createdName.value = res.data.name || '新智能体'
      step.value = 4
      showSuccess.value = true
    } else {
      uni.showToast({ title: '创建失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '创建失败: ' + e.message, icon: 'none' })
  }
  creating.value = false
}

function goChat() {
  showSuccess.value = false
  uni.switchTab({ url: '/pages/dialogue/dialogue' })
}

function createAgain() {
  showSuccess.value = false
  desc.value = ''
  step.value = -1
}
</script>

<style scoped>
.create-page {
  padding: 32rpx;
  min-height: 100vh;
  background: #F8F8F8;
}
.create-header {
  padding-bottom: 16rpx;
}
.create-title {
  font-size: 32rpx;
  font-weight: 600;
  display: block;
  margin-bottom: 12rpx;
  color: #1A1A1A;
}
.create-desc {
  font-size: 26rpx;
  color: #999;
  display: block;
  line-height: 1.5;
}
.create-input-area {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 16rpx 0;
}
.create-input {
  width: 100%;
  min-height: 180rpx;
  font-size: 28rpx;
  line-height: 1.6;
}
.create-info {
  margin: 16rpx 0;
}
.info-title {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
  display: block;
}
.info-item {
  display: inline-block;
  background: #E8F0FE;
  border-radius: 20rpx;
  padding: 8rpx 20rpx;
  margin: 0 8rpx 12rpx 0;
}
.info-tag {
  font-size: 24rpx;
  color: #007AFF;
}
.create-btn {
  height: 88rpx;
  background: #007AFF;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 16rpx;
}
.create-btn.disabled {
  background: #B0D4FF;
}
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal-box {
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx;
  width: 560rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.modal-title {
  font-size: 36rpx;
  font-weight: 600;
  margin: 16rpx 0 8rpx;
}
.modal-desc {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 32rpx;
  text-align: center;
}
.modal-actions {
  display: flex;
  gap: 20rpx;
  width: 100%;
}
.mbtn {
  flex: 1;
  height: 72rpx;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  border: 1rpx solid #007AFF;
  color: #007AFF;
}
.mbtn.primary {
  background: #007AFF;
  color: #fff;
  border: none;
}
</style>
