<template>
  <view class="dialogue">
    <view class="chat-layout">
      <!-- ======== 左侧：会话列表 ======== -->
      <view class="left-panel">
        <AgentSelector :agents="agentStore.agents" :currentAgentId="agentStore.currentAgentId"
          @select="onAgentSelect" compact />

        <!-- 模糊搜索 -->
        <view class="search-box">
          <text class="search-icon">🔍</text>
          <input class="search-input" v-model="searchTerm" placeholder="搜索会话..." confirm-type="search" />
          <text class="search-clear" v-if="searchTerm" @tap="searchTerm = ''">✕</text>
        </view>

        <view class="conv-header">
          <text class="conv-count">会话 ({{ filteredConversations.length }})</text>
          <text class="new-btn" @tap="newConv">＋ 新对话</text>
        </view>

        <scroll-view class="conv-list" scroll-y :show-scrollbar="true">
          <view v-for="c in filteredConversations" :key="c.id"
            class="conv-item" :class="{ active: c.id === chatStore.currentConvId }"
            @tap="selectConversation(c.id)">
            <view class="conv-top">
              <text class="conv-name">{{ c.title }}</text>
              <text class="conv-msg-count">{{ c.messageCount || 0 }}条</text>
            </view>
            <text class="conv-preview">{{ c.lastMessage || '暂无消息' }}</text>
          </view>

          <view v-if="filteredConversations.length === 0" class="conv-empty">
            <text>{{ searchTerm ? '未找到匹配的会话' : '暂无会话，开始新对话吧' }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- ======== 右侧：聊天窗口 ======== -->
      <view class="right-panel">
        <view v-if="!chatStore.currentConvId" class="right-empty">
          <text style="font-size:80rpx">💬</text>
          <text style="font-size:28rpx;color:#999;margin-top:16rpx">选择一个会话或创建新对话</text>
        </view>

        <template v-else>
          <view class="conv-title-bar">
            <text class="ct-title">{{ currentConvTitle }}</text>
          </view>

          <scroll-view class="msg-area" scroll-y :scroll-into-view="scrollId" :scroll-with-animation="true">
            <view v-if="!chatStore.messages.length" class="empty">
              <text style="font-size:64rpx">💬</text>
              <text style="font-size:26rpx;color:#999;margin-top:12rpx">开始新的对话</text>
            </view>
            <MessageBubble v-for="m in chatStore.messages" :key="m.id" :role="m.role" :content="m.content" />
            <MessageBubble v-if="chatStore.streaming" role="assistant" :content="chatStore.streamingContent" :streaming="true" />
            <view id="bottom" />
          </scroll-view>

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
        </template>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useAuthStore } from '@/stores/auth'
import { API_BASE_URL } from '@/config'
import AgentSelector from '@/components/AgentSelector.vue'
import MessageBubble from '@/components/MessageBubble.vue'

const authStore = useAuthStore()
const chatStore = useChatStore()
const agentStore = useAgentStore()

const msg = ref('')
const scrollId = ref('')
const selectedFiles = ref([])
const uploadingFile = ref(false)
const searchTerm = ref('')

// 模糊搜索过滤后的会话列表
const filteredConversations = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  if (!term) return chatStore.conversations
  return chatStore.conversations.filter(c =>
    c.title.toLowerCase().includes(term)
  )
})

// 当前会话标题
const currentConvTitle = computed(() => {
  if (!chatStore.currentConvId) return ''
  const conv = chatStore.conversations.find(c => c.id === chatStore.currentConvId)
  return conv ? conv.title : ''
})

onMounted(() => {
  watch(() => authStore.ready, (val) => {
    if (val && authStore.isLoggedIn) {
      agentStore.loadAgents()
      if (agentStore.currentAgentId) {
        chatStore.loadConversations(agentStore.currentAgentId)
      } else {
        chatStore.loadGeneralConversations()
      }
    }
  }, { immediate: true })
})

// 登录守卫
onShow(() => {
  if (authStore.ready && !authStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
  // 每次显示页面时刷新数据
  if (authStore.ready && authStore.isLoggedIn) {
    agentStore.loadAgents()
    if (agentStore.currentAgentId) {
      chatStore.loadConversations(agentStore.currentAgentId)
    }
  }
})

// 选中会话
function selectConversation(convId) {
  chatStore.switchConversation(convId)
  scrollToBottom()
}

// 新消息到达时自动滚动
watch(() => chatStore.messages.length, () => {
  setTimeout(scrollToBottom, 100)
})
watch(() => chatStore.streamingContent, () => {
  if (chatStore.streaming) scrollToBottom()
})

function scrollToBottom() {
  scrollId.value = 'bottom'
}

function onAgentSelect(agentId) {
  agentStore.selectAgent(agentId)
  chatStore.messages = []
  chatStore.currentConvId = null
  if (agentId) {
    chatStore.loadConversations(agentId)
  } else {
    chatStore.loadGeneralConversations()
  }
  searchTerm.value = ''
}

async function newConv() {
  await chatStore.startNewConversation(agentStore.currentAgentId)
  scrollToBottom()
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
    const res = await uni.chooseFile({ count: 5, type: 'all' })
    const files = res.tempFiles || res.tempFilePaths.map(p => ({ path: p, name: p.split('/').pop() }))
    for (const file of files) {
      await uploadFile(file.path || file, file.name)
    }
  } catch (e) {
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
        filePath,
        name: 'file',
        header: { 'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '') },
        success: (r) => {
          try { resolve(JSON.parse(r.data)) } catch { reject(new Error('解析失败')) }
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
</script>

<style scoped>
.dialogue { display: flex; flex-direction: column; height: 100vh; background: #F8F8F8; }

/* ===== 左右分栏 ===== */
.chat-layout { display: flex; flex: 1; overflow: hidden; }

/* ====== 左侧面板 ====== */
.left-panel { width: 35%; min-width: 200rpx; max-width: 400rpx; border-right: 2rpx solid #F0F0F0; display: flex; flex-direction: column; background: #FAFAFA; }

/* 搜索框 */
.search-box { display: flex; align-items: center; margin: 12rpx 16rpx; padding: 0 20rpx; height: 60rpx; background: #EEEEEE; border-radius: 30rpx; gap: 8rpx; }
.search-icon { font-size: 24rpx; flex-shrink: 0; }
.search-input { flex: 1; font-size: 26rpx; height: 100%; background: transparent; }
.search-clear { font-size: 24rpx; color: #999; padding: 8rpx; flex-shrink: 0; }

/* 会话头部 */
.conv-header { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 20rpx; flex-shrink: 0; }
.conv-count { font-size: 24rpx; color: #999; }
.new-btn { font-size: 26rpx; color: #007AFF; font-weight: 500; }

/* 会话列表 */
.conv-list { flex: 1; overflow-y: auto; padding: 0 12rpx 12rpx; }
.conv-item { padding: 16rpx 16rpx; border-radius: 12rpx; margin-bottom: 4rpx; cursor: pointer; }
.conv-item.active { background: #E8F0FE; }
.conv-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6rpx; }
.conv-name { font-size: 26rpx; font-weight: 500; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 70%; }
.conv-msg-count { font-size: 20rpx; color: #BBB; flex-shrink: 0; }
.conv-preview { font-size: 22rpx; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.conv-empty { padding: 40rpx; text-align: center; font-size: 24rpx; color: #BBB; }

/* ====== 右侧面板 ====== */
.right-panel { flex: 1; display: flex; flex-direction: column; min-width: 0; }

/* 右侧空状态 */
.right-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.right-empty text { display: block; }

/* 当前会话标题栏 */
.conv-title-bar { padding: 16rpx 32rpx; border-bottom: 2rpx solid #F0F0F0; background: #fff; flex-shrink: 0; }
.ct-title { font-size: 28rpx; font-weight: 600; color: #333; }

/* 消息区域 */
.msg-area { flex: 1; overflow-y: auto; }
.empty { display: flex; flex-direction: column; align-items: center; padding-top: 200rpx; }

/* 输入区 */
.input-row { display: flex; align-items: center; padding: 16rpx 24rpx; background: #fff; border-top: 1rpx solid #E5E5E5; gap: 16rpx; flex-shrink: 0; }
.input { flex: 1; height: 72rpx; background: #F5F5F5; border-radius: 36rpx; padding: 0 32rpx; font-size: 28rpx; }
.send-btn { width: 120rpx; height: 72rpx; background: #007AFF; border-radius: 36rpx; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 28rpx; flex-shrink: 0; }
.send-btn.disabled { background: #B0D4FF; }

/* 文件预览 */
.file-preview { display: flex; flex-wrap: wrap; gap: 8rpx; padding: 8rpx 24rpx; background: #fff; border-top: 1rpx solid #E5E5E5; }
.file-item { display: flex; align-items: center; background: #F0F4FF; border-radius: 12rpx; padding: 8rpx 16rpx; gap: 8rpx; }
.file-icon { font-size: 24rpx; }
.file-name { font-size: 24rpx; color: #333; max-width: 200rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-del { font-size: 20rpx; color: #999; padding: 4rpx; }
.attach-btn { width: 56rpx; height: 56rpx; display: flex; align-items: center; justify-content: center; font-size: 40rpx; flex-shrink: 0; }

</style>
