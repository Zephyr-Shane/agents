<template>
  <view class="dialogue">
    <!-- 顶部子Tab切换 -->
    <view class="sub-tabs">
      <view class="sub-tab" :class="{ active: activeTab === 'chat' }" @tap="activeTab='chat'">普通对话</view>
      <view class="sub-tab" :class="{ active: activeTab === 'create' }" @tap="activeTab='create'">创建智能体</view>
    </view>

    <!-- ===== 普通对话 ===== -->
    <block v-if="activeTab === 'chat'">
      <AgentSelector :agents="agentStore.agents" :currentAgentId="agentStore.currentAgentId" @select="onAgentSelect" />

      <view class="conv-bar">
        <text class="conv-title">会话列表</text>
        <text class="new-btn" @tap="newConv">＋ 新对话</text>
      </view>

      <scroll-view class="conv-list" scroll-y v-if="chatStore.conversations.length > 0">
        <view v-for="c in chatStore.conversations" :key="c.id"
          class="conv-item" :class="{ active: c.id === chatStore.currentConvId }"
          @tap="chatStore.switchConversation(c.id)">
          <text class="conv-name">{{ c.title }}</text>
        </view>
      </scroll-view>

      <scroll-view class="msg-area" scroll-y :scroll-into-view="scrollId">
        <view v-if="!chatStore.messages.length" class="empty">
          <text style="font-size:80rpx">💬</text>
          <text style="font-size:28rpx;color:#999;margin-top:16rpx">开始一段新的对话</text>
        </view>
        <MessageBubble v-for="m in chatStore.messages" :key="m.id" :role="m.role" :content="m.content" />
        <MessageBubble v-if="chatStore.streaming" role="assistant" :content="chatStore.streamingContent" :streaming="true" />
        <view id="bottom" />
      </scroll-view>

      <!-- 已选文件列表 -->
      <view v-if="selectedFiles.length > 0" class="file-preview">
        <view v-for="(f, i) in selectedFiles" :key="i" class="file-item">
          <text class="file-icon">📎</text>
          <text class="file-name">{{ f.name }}</text>
          <text class="file-del" @tap="removeFile(i)">✕</text>
        </view>
      </view>
      <view class="input-row">
        <view class="attach-btn" @tap="chooseFile" v-if="!chatStore.streaming">📎</view>
        <input class="input" v-model="msg" placeholder="输入消息..." :disabled="chatStore.streaming" @confirm="send" confirm-type="send" />
        <view class="send-btn" :class="{ disabled: !msg.trim() || chatStore.streaming }" @tap="send">发送</view>
      </view>
    </block>

    <!-- ===== 创建智能体 ===== -->
    <block v-if="activeTab === 'create'">
      <view class="create-wrap">
        <view class="create-header">
          <text class="create-title">用自然语言描述你想要的智能体</text>
          <text class="create-desc">例如：「创建一个PDF文档知识库助手，只能读取上传文件回答，禁止联网」</text>
        </view>
        <view class="create-input-area">
          <textarea class="create-input" v-model="desc" placeholder="描述智能体的功能、风格、约束..." :disabled="creating" auto-height />
        </view>
        <view class="create-btn" :class="{ disabled: !desc.trim() || creating }" @tap="doCreate">{{ creating ? '创建中...' : '开始创建' }}</view>
        <StepIndicator v-if="creating" :steps="steps" :currentStep="step" :loading="creating" />
      </view>
    </block>

    <!-- 成功弹窗 -->
    <view v-if="showSuccess" class="modal" @tap="showSuccess=false">
      <view class="modal-box" @tap.stop>
        <text style="font-size:64rpx">🎉</text>
        <text class="modal-title">创建完成</text>
        <text class="modal-desc">智能体「{{ createdName }}」已创建成功！</text>
        <view class="modal-actions">
          <view class="mbtn" @tap="useNow">立即使用</view>
          <view class="mbtn primary" @tap="createAnother">继续创建</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useAuthStore } from '@/stores/auth'
import { createAgentFromNL } from '@/api/agent'
import { API_BASE_URL } from '@/config'
import AgentSelector from '@/components/AgentSelector.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import StepIndicator from '@/components/StepIndicator.vue'

const authStore = useAuthStore()
const chatStore = useChatStore()
const agentStore = useAgentStore()

const activeTab = ref('chat')
const msg = ref('')
const desc = ref('')
const creating = ref(false)
const step = ref(-1)
const showSuccess = ref(false)
const createdName = ref('')
const steps = ['正在解析角色定位', '正在配置知识库能力', '正在配置联网权限', '智能体创建完成']
const scrollId = ref('')
// 文件上传相关
const selectedFiles = ref([])       // { id, name }
const uploadingFile = ref(false)

onMounted(() => {
  watch(() => authStore.ready, (val) => {
    if (val && authStore.isLoggedIn) {
      agentStore.loadAgents()
      chatStore.loadConversations(agentStore.currentAgentId)
    }
  }, { immediate: true })
})

// 登录守卫：未登录时跳转到登录页
onShow(() => {
  if (authStore.ready && !authStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
})

function onAgentSelect(agentId) {
  agentStore.selectAgent(agentId)
  chatStore.messages = []
  chatStore.currentConvId = null
  chatStore.loadConversations(agentId)
}

async function newConv() {
  await chatStore.startNewConversation(agentStore.currentAgentId)
}

function send() {
  const text = msg.value.trim()
  if (!text || chatStore.streaming) return
  msg.value = ''
  const fileIds = selectedFiles.value.map(f => f.id)
  selectedFiles.value = []
  chatStore.sendMessage(agentStore.currentAgentId, text, fileIds)
}

async function chooseFile() {
  if (!agentStore.currentAgentId) {
    uni.showToast({ title: '请先选择一个智能体', icon: 'none' })
    return
  }
  try {
    const res = await uni.chooseFile({
      count: 5,
      type: 'all'
    })
    // uni-app H5 返回 tempFiles
    const files = res.tempFiles || res.tempFilePaths.map(p => ({ path: p, name: p.split('/').pop() }))
    for (const file of files) {
      await uploadFile(file.path || file, file.name)
    }
  } catch (e) {
    // 用户取消选择不报错
    if (e?.errMsg && !e.errMsg.includes('cancel')) {
      console.error('选择文件失败:', e)
    }
  }
}

async function uploadFile(filePath, fileName) {
  uploadingFile.value = true
  try {
    const res = await new Promise((resolve, reject) => {
      uni.uploadFile({
        url: API_BASE_URL + '/knowledge/upload/' + agentStore.currentAgentId,
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '')
        },
        success: (r) => {
          try {
            resolve(JSON.parse(r.data))
          } catch {
            reject(new Error('上传返回解析失败'))
          }
        },
        fail: reject
      })
    })
    if (res.code === 0 && res.data) {
      selectedFiles.value.push({ id: res.data.id, name: fileName })
      uni.showToast({ title: '文件上传成功', icon: 'success' })
    } else {
      uni.showToast({ title: '上传失败: ' + (res.message || '未知错误'), icon: 'none' })
    }
  } catch (e) {
    console.error('上传文件异常:', e)
    uni.showToast({ title: '上传失败: ' + (e.message || '网络错误'), icon: 'none' })
  } finally {
    uploadingFile.value = false
  }
}

function removeFile(index) {
  selectedFiles.value.splice(index, 1)
}

async function doCreate() {
  if (!desc.value.trim() || creating.value) return
  creating.value = true
  step.value = 0
  try {
    const res = await createAgentFromNL(desc.value.trim())
    if (res && res.code === 0 && res.data) {
      createdName.value = res.data.name || '新智能体'
      step.value = 4
      agentStore.loadAgents()
      showSuccess.value = true
    } else {
      uni.showToast({ title: '创建失败', icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: '创建失败: ' + e.message, icon: 'none' })
  }
  creating.value = false
}

function useNow() {
  showSuccess.value = false
  activeTab.value = 'chat'
  if (agentStore.agents.length > 0) onAgentSelect(agentStore.agents[0].id)
}

function createAnother() {
  showSuccess.value = false
  desc.value = ''
  step.value = -1
}
</script>

<style scoped>
.dialogue { display: flex; flex-direction: column; height: 100vh; }
.sub-tabs { display: flex; background: #fff; padding: 20rpx 0 0; position: sticky; top: 0; z-index: 10; }
.sub-tab { flex: 1; text-align: center; font-size: 30rpx; color: #666; padding: 12rpx 0 20rpx; position: relative; }
.sub-tab.active { color: #333; font-weight: 600; }
.sub-tab.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 48rpx; height: 4rpx; background: #007AFF; border-radius: 2rpx; }

.conv-bar { display: flex; justify-content: space-between; padding: 16rpx 32rpx; }
.conv-title { font-size: 26rpx; color: #999; }
.new-btn { font-size: 26rpx; color: #007AFF; }
.conv-list { max-height: 200rpx; padding: 0 32rpx; }
.conv-item { padding: 16rpx 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 8rpx; }
.conv-item.active { background: #E8F0FE; }
.conv-name { font-size: 26rpx; }

.msg-area { flex: 1; overflow-y: auto; }
.empty { display: flex; flex-direction: column; align-items: center; padding-top: 200rpx; }
.input-row { display: flex; align-items: center; padding: 16rpx 24rpx; background: #fff; border-top: 1rpx solid #E5E5E5; gap: 16rpx; }
.input { flex: 1; height: 72rpx; background: #F5F5F5; border-radius: 36rpx; padding: 0 32rpx; font-size: 28rpx; }
.send-btn { width: 120rpx; height: 72rpx; background: #007AFF; border-radius: 36rpx; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 28rpx; }
.send-btn.disabled { background: #B0D4FF; }

/* 文件上传预览 */
.file-preview { display: flex; flex-wrap: wrap; gap: 8rpx; padding: 8rpx 24rpx; background: #fff; border-top: 1rpx solid #E5E5E5; }
.file-item { display: flex; align-items: center; background: #F0F4FF; border-radius: 12rpx; padding: 8rpx 16rpx; gap: 8rpx; }
.file-icon { font-size: 24rpx; }
.file-name { font-size: 24rpx; color: #333; max-width: 200rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-del { font-size: 20rpx; color: #999; padding: 4rpx; }
.attach-btn { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; font-size: 40rpx; flex-shrink: 0; }

.create-wrap { padding: 32rpx; flex: 1; }
.create-title { font-size: 32rpx; font-weight: 600; display: block; margin-bottom: 12rpx; }
.create-desc { font-size: 26rpx; color: #999; display: block; }
.create-input-area { background: #fff; border-radius: 16rpx; padding: 24rpx; margin: 24rpx 0; }
.create-input { width: 100%; min-height: 200rpx; font-size: 28rpx; }
.create-btn { height: 88rpx; background: #007AFF; border-radius: 44rpx; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32rpx; font-weight: 600; }
.create-btn.disabled { background: #B0D4FF; }

.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-box { background: #fff; border-radius: 24rpx; padding: 48rpx; width: 560rpx; display: flex; flex-direction: column; align-items: center; }
.modal-title { font-size: 36rpx; font-weight: 600; margin: 16rpx 0 8rpx; }
.modal-desc { font-size: 28rpx; color: #666; margin-bottom: 32rpx; text-align: center; }
.modal-actions { display: flex; gap: 20rpx; width: 100%; }
.mbtn { flex: 1; height: 72rpx; border-radius: 36rpx; display: flex; align-items: center; justify-content: center; font-size: 28rpx; border: 1rpx solid #007AFF; color: #007AFF; }
.mbtn.primary { background: #007AFF; color: #fff; border: none; }
</style>
