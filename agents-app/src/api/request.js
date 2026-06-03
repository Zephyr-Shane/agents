const BASE_URL = '/api'

function getToken() {
  return localStorage.getItem('token') || ''
}

async function request(options) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(BASE_URL + options.url, {
    method: options.method || 'GET',
    headers,
    body: options.data ? JSON.stringify(options.data) : undefined
  })

  if (res.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.reload()
    throw new Error('未登录')
  }

  return res.json()
}

export function get(url) {
  return request({ url, method: 'GET' })
}

export function post(url, data) {
  return request({ url, method: 'POST', data })
}

export function del(url) {
  return request({ url, method: 'DELETE' })
}
