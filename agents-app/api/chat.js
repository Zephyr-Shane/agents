import { get, post, postStream } from './request'
import { extractChatResponse } from '@/utils/sse-parser'

export function getConversations(agentId) {
  const params = agentId != null ? '?agentId=' + agentId : ''
  return get('/conversations' + params)
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
