import { post, get } from './request'
import { API_BASE_URL } from '@/config'

export function loginByPasswordApi(username, password) {
  return post('/auth/password-login', { username, password })
}

export function registerApi(username, password, nickname) {
  return post('/auth/register', { username, password, nickname })
}

export function updateProfileApi(nickname, avatar) {
  return post('/auth/update-profile', { nickname, avatar })
}

export function getProfile() {
  return get('/auth/profile')
}

export function uploadAvatarApi(filePath) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.uploadFile({
      url: API_BASE_URL + '/upload/avatar',
      filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + token },
      success: (res) => {
        try { resolve(JSON.parse(res.data)) }
        catch { reject(new Error('解析上传结果失败')) }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '上传失败'))
      }
    })
  })
}
