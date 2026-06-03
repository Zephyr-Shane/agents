import { post, get } from './request'

export function loginApi(code) {
  return post('/auth/login', { code })
}

export function getProfile() {
  return get('/auth/profile')
}
