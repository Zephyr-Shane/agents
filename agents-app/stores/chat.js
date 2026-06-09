import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConversations, getGeneralConversations, createConversation, getMessages, streamChat, streamChatChunked } from '@/api/chat'

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
    messages.value = []
    currentConvId.value = convId
    const res = await getMessages(convId)
    if (res.code === 0) messages.value = res.data
  }

  /** 重新加载当前对话的消息（保留已有内容直到接口返回，避免闪白） */
  async function refreshMessages() {
    const convId = currentConvId.value
    if (!convId) return
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
      messages.value = []
      currentConvId.value = convId
      conversations.value.unshift(res.data)
    }

    messages.value.push({ id: Date.now(), role: 'user', content: text })
    streaming.value = true
    streamingContent.value = ''

    try {
      let content, conversationId
      try {
        const result = await streamChatChunked({
          conversationId: convId,
          agentId,
          message: text,
          fileIds: fileIds || []
        }, (token) => {
          streamingContent.value += token
        })
        content = result.content
        conversationId = result.conversationId
      } catch (chunkErr) {
        console.warn('Chunked stream unavailable, falling back to regular SSE:', chunkErr.message)
        const result = await streamChat({
          conversationId: convId,
          agentId,
          message: text,
          fileIds: fileIds || []
        })
        content = result.content
        conversationId = result.conversationId
      }

      streaming.value = false
      streamingContent.value = ''

      if (conversationId && conversationId !== convId) {
        currentConvId.value = conversationId
      }

      if (content) {
        messages.value.push({ id: Date.now() + 1, role: 'assistant', content })
      }
    } catch (e) {
      streaming.value = false
      streamingContent.value = '发送失败: ' + (e.message || '请重试')
      console.error('chat sendMessage error:', e)
    }
  }

  return {
    conversations, currentConvId, messages, streaming, streamingContent,
    loadConversations, loadGeneralConversations,
    startNewConversation, switchConversation, sendMessage, refreshMessages
  }
})
