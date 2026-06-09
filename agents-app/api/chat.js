import { get, post, postStream, postStreamChunked } from './request'
import { extractChatResponse } from '@/utils/sse-parser'

export function getConversations(agentId) {
  const params = agentId != null ? '?agentId=' + agentId : '?type=general'
  return get('/conversations' + params)
}
export function getGeneralConversations() {
  return get('/conversations?type=general')
}
export function createConversation(data) { return post('/conversations', data) }
export function getMessages(convId) { return get('/conversations/' + convId + '/messages') }

/**
 * 发送聊天消息并获取流式响应
 * 后端使用 SSE，前端解析完整响应提取最终内容
 * 返回格式: { content, conversationId, error }
 */
export async function streamChat(data) {
  const rawText = await postStream('/chat/stream', data)
  const result = extractChatResponse(rawText)
  if (result.error) {
    throw new Error(result.error)
  }
  return result
}

/**
 * 流式聊天 —— onToken 逐字回调，返回 Promise<{ content, conversationId }>
 */
export function streamChatChunked(data, onToken) {
  return postStreamChunked('/chat/stream', data, onToken)
}
