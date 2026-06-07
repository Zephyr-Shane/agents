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
      reject(err)
    }
  })
}

export function get(url) { return request({ url, method: 'GET' }) }
export function post(url, data) { return request({ url, method: 'POST', data }) }
export function postStream(url, data) { return request({ url, method: 'POST', data, rawResponse: true }) }
export function del(url) { return request({ url, method: 'DELETE' }) }
