import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConversations, getGeneralConversations, createConversation, getMessages, streamChat } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref([])
  const currentConvId = ref(null)
  const messages = ref([])
  const streaming = ref(false)
  const streamingContent = ref('')

  /** 加载智能体对话列表（agentId 有值）或普通对话列表（agentId 为空） */
  async function loadConversations(agentId) {
    if (agentId) {
      const res = await getConversations(agentId)
      if (res.code === 0) conversations.value = res.data
    } else {
      await loadGeneralConversations()
    }
  }

  /** 加载普通对话列表（type=general） */
  async function loadGeneralConversations() {
    const res = await getGeneralConversations()
    if (res.code === 0) conversations.value = res.data
  }

  async function startNewConversation(agentId) {
    const data = agentId ? { agentId, type: 'agent' } : { type: 'general' }
    const res = await createConversation(data)
    if (res.code === 0) {
      conversations.value.unshift(res.data)
      await switchConversation(res.data.id)
    }
  }

  async function switchConversation(convId) {
    currentConvId.value = convId
    const res = await getMessages(convId)
    if (res.code === 0) messages.value = res.data
  }

  async function sendMessage(agentId, text, fileIds) {
    let convId = currentConvId.value
    if (!convId) {
      const data = agentId ? { agentId, type: 'agent' } : { type: 'general' }
      const res = await createConversation(data)
      if (res.code !== 0) return
      convId = res.data.id
      currentConvId.value = convId
      conversations.value.unshift(res.data)
    }

    messages.value.push({ id: Date.now(), role: 'user', content: text })
    streaming.value = true
    streamingContent.value = ''

    try {
      const res = await streamChat({
        conversationId: convId,
        agentId,
        message: text,
        fileIds: fileIds || []
      })
      if (res.content) {
        streamingContent.value = res.content
      }
      if (res.error) {
        streamingContent.value = '发送失败: ' + res.error
      }
      if (res.conversationId && res.conversationId !== convId) {
        currentConvId.value = res.conversationId
      }
    } catch (e) {
      streamingContent.value = '发送失败，请重试'
      console.error('chat sendMessage error:', e)
    }

    if (streamingContent.value) {
      messages.value.push({ id: Date.now() + 1, role: 'assistant', content: streamingContent.value })
    }
    streaming.value = false
    streamingContent.value = ''
  }

  return {
    conversations, currentConvId, messages, streaming, streamingContent,
    loadConversations, loadGeneralConversations,
    startNewConversation, switchConversation, sendMessage
  }
})
