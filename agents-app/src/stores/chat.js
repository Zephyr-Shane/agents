import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConversations, createConversation, getMessages, streamChat } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref([])
  const currentConvId = ref(null)
  const messages = ref([])
  const streaming = ref(false)
  const streamingContent = ref('')

  async function loadConversations(agentId) {
    const res = await getConversations(agentId)
    if (res.code === 0) conversations.value = res.data
  }

  async function startNewConversation(agentId) {
    await loadConversations(agentId)
    const res = await createConversation({ agentId })
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

  async function sendMessage(agentId, text) {
    let convId = currentConvId.value
    if (!convId) {
      const res = await createConversation({ agentId })
      if (res.code !== 0) return
      convId = res.data.id
      currentConvId.value = convId
      conversations.value.unshift(res.data)
    }

    messages.value.push({ id: Date.now(), role: 'user', content: text })
    streaming.value = true
    streamingContent.value = ''

    try {
      const res = await streamChat({ conversationId: convId, agentId, message: text })
      streamingContent.value = typeof res === 'string' ? res : JSON.stringify(res)
    } catch (e) {
      streamingContent.value = '发送失败，请重试'
    }

    if (streamingContent.value) {
      messages.value.push({ id: Date.now() + 1, role: 'assistant', content: streamingContent.value })
    }
    streaming.value = false
    streamingContent.value = ''
  }

  return {
    conversations, currentConvId, messages, streaming, streamingContent,
    loadConversations, startNewConversation, switchConversation, sendMessage
  }
})
