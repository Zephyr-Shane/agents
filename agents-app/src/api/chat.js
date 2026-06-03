import { get, post } from './request'

export function getConversations(agentId) {
  const params = agentId != null ? `?agentId=${agentId}` : ''
  return get('/conversations' + params)
}

export function createConversation(data) {
  return post('/conversations', data)
}

export function getMessages(conversationId) {
  return get(`/conversations/${conversationId}/messages`)
}

export function streamChat(data) {
  return post('/chat/stream', data)
}
