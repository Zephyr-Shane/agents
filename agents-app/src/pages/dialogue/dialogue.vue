<template>
  <div class="page">
    <!-- Sub-tabs -->
    <div class="sub-tabs">
      <span class="sub-tab" :class="{active: t === 'chat'}" @click="t='chat'">普通对话</span>
      <span class="sub-tab" :class="{active: t === 'create'}" @click="t='create'">创建智能体</span>
    </div>

    <!-- ===== 普通对话 ===== -->
    <template v-if="t === 'chat'">
      <AgentSelector :agents="agentStore.agents" :currentAgentId="agentStore.currentAgentId" />
      <div class="conv-bar">
        <span>会话列表</span>
        <span class="link" @click="startNew">＋ 新对话</span>
      </div>

      <div class="msg-area" ref="msgArea">
        <div v-if="!chatStore.messages.length" class="empty">
          <div style="font-size:40px;margin-bottom:8px">💬</div>
          <div style="color:#999">开始一段新的对话</div>
        </div>
        <MessageBubble v-for="m in chatStore.messages" :key="m.id" :role="m.role" :content="m.content" />
        <MessageBubble v-if="chatStore.streaming" role="assistant" :content="chatStore.streamingContent" :streaming="true" />
      </div>

      <div class="input-row">
        <input class="input" v-model="msg" placeholder="输入消息..." :disabled="chatStore.streaming" @keyup.enter="send" />
        <button class="send" :disabled="!msg.trim()||chatStore.streaming" @click="send">发送</button>
      </div>
    </template>

    <!-- ===== 创建智能体 ===== -->
    <template v-if="t === 'create'">
      <div style="padding:20px 16px">
        <h3 style="font-size:16px;margin-bottom:8px">用自然语言描述你想要的智能体</h3>
        <p style="font-size:13px;color:#999;margin-bottom:20px">例如：「创建一个PDF文档知识库助手，只能读取上传文件回答，禁止联网」</p>
        <textarea class="create-input" v-model="desc" placeholder="描述智能体的功能、风格、约束..." :disabled="creating" rows="5" />
        <button class="create-btn" :disabled="!desc.trim()||creating" @click="doCreate">{{ creating ? '创建中...' : '开始创建' }}</button>
        <StepIndicator v-if="creating" :steps="steps" :currentStep="step" :loading="creating" />
      </div>
    </template>

    <!-- Success modal -->
    <div v-if="showSuccess" class="modal" @click="showSuccess=false">
      <div class="modal-box" @click.stop>
        <div style="font-size:32px;margin-bottom:8px">🎉</div>
        <h3>创建完成</h3>
        <p style="margin:8px 0 20px;color:#666">智能体「{{ createdName }}」已创建成功！</p>
        <div style="display:flex;gap:10px">
          <button class="btn" @click="useNow">立即使用</button>
          <button class="btn primary" @click="createAnother">继续创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { createAgentFromNL } from '@/api/agent'
import AgentSelector from '@/components/AgentSelector.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import StepIndicator from '@/components/StepIndicator.vue'

const t = ref('chat')
const msg = ref('')
const desc = ref('')
const creating = ref(false)
const step = ref(-1)
const showSuccess = ref(false)
const createdName = ref('')
const steps = ['正在解析角色定位', '正在配置知识库能力', '正在配置联网权限', '智能体创建完成']

const chatStore = useChatStore()
const agentStore = useAgentStore()

onMounted(() => {
  agentStore.loadAgents()
  chatStore.loadConversations(agentStore.currentAgentId)
})

async function startNew() {
  await chatStore.startNewConversation(agentStore.currentAgentId)
}

function send() {
  const text = msg.value.trim()
  if (!text) return
  msg.value = ''
  chatStore.sendMessage(agentStore.currentAgentId, text)
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
      alert('创建失败')
    }
  } catch (e) {
    alert('创建失败: ' + e.message)
  }
  creating.value = false
}

function useNow() {
  showSuccess.value = false
  t.value = 'chat'
}

function createAnother() {
  showSuccess.value = false
  desc.value = ''
  step.value = -1
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.sub-tabs { display: flex; background: #fff; padding: 12px 0 0; position: sticky; top: 0; z-index: 10; }
.sub-tab { flex:1; text-align:center; font-size:15px; color:#666; padding:8px 0 14px; cursor:pointer; position:relative; }
.sub-tab.active { color:#333; font-weight:600; }
.sub-tab.active::after { content:''; position:absolute; bottom:0; left:50%; transform:translateX(-50%); width:24px; height:3px; background:#007AFF; border-radius:2px; }
.conv-bar { display:flex; justify-content:space-between; padding:10px 16px; font-size:13px; color:#999; }
.link { color:#007AFF; cursor:pointer; }
.msg-area { flex:1; overflow-y:auto; padding:8px 0; }
.empty { display:flex; flex-direction:column; align-items:center; padding-top:100px; }
.input-row { display:flex; align-items:center; padding:10px 12px; background:#fff; border-top:1px solid #E5E5E5; gap:8px; }
.input { flex:1; height:36px; padding:0 14px; border-radius:18px; border:none; background:#F5F5F5; font-size:14px; outline:none; }
.send { height:36px; padding:0 18px; border-radius:18px; border:none; background:#007AFF; color:#fff; font-size:14px; cursor:pointer; }
.send:disabled { background:#B0D4FF; cursor:default; }
.create-input { width:100%; padding:12px; border:1px solid #E5E5E5; border-radius:12px; font-size:14px; outline:none; resize:vertical; margin-bottom:16px; }
.create-btn { width:100%; height:44px; border-radius:22px; border:none; background:#007AFF; color:#fff; font-size:16px; font-weight:600; cursor:pointer; margin-bottom:16px; }
.create-btn:disabled { background:#B0D4FF; cursor:default; }
.modal { position:fixed; inset:0; background:rgba(0,0,0,0.5); display:flex; align-items:center; justify-content:center; z-index:200; }
.modal-box { background:#fff; border-radius:16px; padding:32px; width:320px; text-align:center; }
.btn { flex:1; height:40px; border-radius:20px; border:1px solid #007AFF; color:#007AFF; background:transparent; font-size:14px; cursor:pointer; }
.btn.primary { background:#007AFF; color:#fff; border:none; }
</style>
