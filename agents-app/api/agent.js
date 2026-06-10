import { get, post, put, del, postStream } from './request'
import { parseSSEText } from '@/utils/sse-parser'

export function getMyAgents() { return get('/agents') }
export function getAgentDetail(id) { return get('/agents/' + id) }

/**
 * 结构化创建智能体（手动表单）
 */
export function createAgent(data) {
  return post('/agents', data)
}

/**
 * 更新智能体设置
 */
export function updateAgent(id, data) {
  return put('/agents/' + id, data)
}

/**
 * 自然语言创建智能体（SSE 流式）
 */
export async function createAgentFromNL(desc) {
  try {
    const rawText = await postStream('/agents/create-from-nl', { description: desc })
    const events = parseSSEText(rawText)

    // 查找最后一个 completed 事件
    const completedEvent = events.find(e => e.data?.type === 'completed')
    const errorEvent = events.find(e => e.data?.type === 'error')

    if (errorEvent) {
      return { code: 1002, message: errorEvent.data?.content || '创建失败', data: null }
    }

    if (completedEvent) {
      return {
        code: 0,
        message: 'ok',
        data: {
          id: completedEvent.data?.agentId,
          name: completedEvent.data?.name || completedEvent.data?.message || '新智能体'
        }
      }
    }

    // 如果没有 completed 和 error 事件，返回所有事件的原始文本以便调试
    console.warn('创建智能体未收到 completed 事件，原始响应:', rawText)
    return { code: 1002, message: '创建失败：未收到完成信号', data: null }
  } catch (e) {
    console.error('创建智能体异常:', e)
    return { code: 1002, message: '创建失败: ' + (e.message || '网络错误'), data: null }
  }
}

/**
 * 自然语言更新智能体
 */
export async function updateAgentFromNL(agentId, desc) {
  try {
    const rawText = await postStream('/agents/update-from-nl', { agentId, description: desc })
    const events = parseSSEText(rawText)

    const completedEvent = events.find(e => e.data?.type === 'completed')
    const errorEvent = events.find(e => e.data?.type === 'error')

    if (errorEvent) {
      return { code: 1002, message: errorEvent.data?.content || '更新失败', data: null }
    }

    if (completedEvent) {
      return { code: 0, message: 'ok', data: { id: agentId } }
    }

    return { code: 1002, message: '更新失败：未收到完成信号', data: null }
  } catch (e) {
    console.error('更新智能体异常:', e)
    return { code: 1002, message: '更新失败: ' + (e.message || '网络错误'), data: null }
  }
}

export function deleteAgent(id) { return del('/agents/' + id) }

/** 回滚智能体到上一版本 */
export function rollbackAgent(id) { return post('/agents/' + id + '/rollback', {}) }
