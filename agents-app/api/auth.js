import { post, get } from './request'

export function loginApi(code) { return post('/auth/login', { code }) }
export function phoneLoginApi(data) { return post('/auth/phone-login', data) }
export function getProfile() { return get('/auth/profile') }
