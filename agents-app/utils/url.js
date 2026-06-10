import { API_BASE_URL } from '@/config'

const BASE_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '')

export function resolveAvatarUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return BASE_ORIGIN + url
}
