/**
 * SSE 响应解析工具
 * 将 SSE (Server-Sent Events) 格式的纯文本解析为事件数组
 */

/**
 * 解析 SSE 文本为事件对象数组
 * @param {string} text - 原始 SSE 文本
 * @returns {Array<{event: string, data: object|string}>}
 */
export function parseSSEText(text) {
  if (!text || typeof text !== 'string') return []

  const events = []
  const lines = text.split('\n')
  let currentEvent = 'message'
  let currentData = ''

  for (const line of lines) {
    if (line.startsWith('event:')) {
      currentEvent = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      currentData += line.slice(5).trim()
    } else if (line === '' && currentData) {
      // 空行表示事件结束
      try {
        events.push({
          event: currentEvent,
          data: JSON.parse(currentData)
        })
      } catch {
        events.push({
          event: currentEvent,
          data: currentData
        })
      }
      currentEvent = 'message'
      currentData = ''
    }
  }

  // 处理最后可能没有空行结尾的情况
  if (currentData) {
    try {
      events.push({
        event: currentEvent,
        data: JSON.parse(currentData)
      })
    } catch {
      events.push({
        event: currentEvent,
        data: currentData
      })
    }
  }

  return events
}

/**
 * 从 SSE 响应文本中提取完整的 assistant 回复内容
 * @param {string} text - 原始 SSE 文本
 * @returns {{ content: string, conversationId: number|null, error: string|null }}
 */
export function extractChatResponse(text) {
  const events = parseSSEText(text)
  let content = ''
  let conversationId = null
  let error = null

  for (const evt of events) {
    if (evt.data?.type === 'token') {
      content += evt.data.content || ''
    } else if (evt.data?.type === 'done') {
      conversationId = evt.data.conversationId
    } else if (evt.data?.type === 'error') {
      error = evt.data.content || '未知错误'
    }
  }

  return { content, conversationId, error }
}
