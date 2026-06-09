import { API_BASE_URL } from '@/config'

// 注意：动态导入 useAuthStore 以打破循环依赖 stores/auth ← api/auth ← api/request ← stores/auth
function getAuthStore() {
  // 在函数内延迟获取 store，避免启动时的循环依赖
  return import('@/stores/auth').then(m => m.useAuthStore())
}

const BASE_URL = API_BASE_URL

let refreshing = false
const queue = []

function getToken() {
  try { return uni.getStorageSync('token') || '' }
  catch { return '' }
}

function request(options) {
  return new Promise((resolve, reject) => {
    doRequest(options, resolve, reject)
  })
}

function doRequest(options, resolve, reject) {
  const token = getToken()
  const header = { ...options.header }
  // 对于 SSE 请求，不要设置 Content-Type
  if (!options.rawResponse) {
    header['Content-Type'] = 'application/json'
  }
  if (token) header['Authorization'] = 'Bearer ' + token

  const reqOptions = {
    url: BASE_URL + options.url,
    method: options.method || 'GET',
    data: options.data,
    header,
    timeout: options.rawResponse ? 300000 : 8000, // SSE 请求超时设为 5 分钟
  }
  // SSE 请求需要返回原始文本
  if (options.rawResponse) {
    reqOptions.responseType = 'text'
  }

  uni.request({
    ...reqOptions,
    success: async (res) => {
      if (res.statusCode === 401) {
        if (!refreshing) {
          refreshing = true
          try {
            const auth = await getAuthStore()
            await auth.reLogin()
            queue.forEach(q => doRequest(q.opts, q.resolve, q.reject))
            queue.length = 0
            doRequest(options, resolve, reject)
          } catch {
            queue.forEach(q => q.reject(new Error('登录失败')))
            queue.length = 0
            reject(new Error('登录失败'))
          } finally {
            refreshing = false
          }
        } else {
          queue.push({ opts: options, resolve, reject })
        }
        return
      }
      // SSE 请求返回原始文本
      if (options.rawResponse) {
        resolve(res.data)
      } else {
        resolve(res.data)
      }
    },
    fail: (err) => {
      // 将 uni-app 错误对象转为更有意义的 Error
      const errMsg = err?.errMsg || err?.message || ''
      let message = '网络请求失败'
      if (errMsg.includes('timeout')) message = '请求超时，请检查后端服务'
      else if (errMsg.includes('fail')) message = '连接失败，请检查后端是否运行'
      console.error('API请求失败:', options.url, errMsg)
      reject(new Error(message))
    }
  })
}

export function get(url) { return request({ url, method: 'GET' }) }
export function post(url, data) { return request({ url, method: 'POST', data }) }
export function put(url, data) { return request({ url, method: 'PUT', data }) }
export function postStream(url, data) { return request({ url, method: 'POST', data, rawResponse: true }) }
export function del(url) { return request({ url, method: 'DELETE' }) }

function arrayBufferToString(buf) {
  const uint8 = new Uint8Array(buf)
  if (typeof TextDecoder !== 'undefined') {
    return new TextDecoder().decode(uint8)
  }
  let binary = ''
  for (let i = 0; i < uint8.length; i++) {
    binary += String.fromCharCode(uint8[i])
  }
  return decodeURIComponent(escape(binary))
}

/**
 * 基于 WeChat 原生 wx.request + enableChunked 的 SSE 流式请求
 * 逐 token 回调 onToken，完成后 resolve({ content, conversationId })
 */
export function postStreamChunked(url, data, onToken) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const url_ = BASE_URL + url

    if (typeof wx !== 'undefined' && wx.request) {
      let partial = ''
      let fullContent = ''
      let settled = false

      const task = wx.request({
        url: url_,
        method: 'POST',
        data,
        header: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + token
        },
        enableChunked: true,
        success: () => {
          if (!settled) {
            settled = true
            resolve({ content: fullContent, conversationId: null })
          }
        },
        fail: (err) => {
          if (!settled) {
            settled = true
            reject(new Error(err.errMsg || '请求失败'))
          }
        }
      })

      task.onChunkReceived((response) => {
        if (settled) return
        const chunk = arrayBufferToString(response.data).replace(/\r\n/g, '\n')
        partial += chunk

        while (true) {
          const idx = partial.indexOf('\n\n')
          if (idx === -1) break
          const rawEvent = partial.slice(0, idx)
          partial = partial.slice(idx + 2)

          const lines = rawEvent.split('\n')
          let dataStr = ''
          for (const line of lines) {
            const t = line.trim()
            if (t.startsWith('data:')) {
              dataStr = t.slice(5).trim()
            }
          }
          if (!dataStr) continue

          try {
            const parsed = JSON.parse(dataStr)
            if (parsed.type === 'token') {
              fullContent += parsed.content || ''
              onToken?.(parsed.content || '')
            } else if (parsed.type === 'done') {
              settled = true
              resolve({ content: fullContent, conversationId: parsed.conversationId })
            } else if (parsed.type === 'error') {
              settled = true
              reject(new Error(parsed.content || '未知错误'))
            }
          } catch (_) { /* skip partial JSON */ }
        }
      })
    } else {
      reject(new Error('当前环境不支持流式请求'))
    }
  })
}
