import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  loginByPasswordApi, registerApi,
  getProfile, updateProfileApi
} from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const user = ref(null)
  const ready = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  function init() {
    try {
      const t = uni.getStorageSync('token')
      if (t) token.value = t
      const u = uni.getStorageSync('user')
      if (u) user.value = JSON.parse(u)
    } catch {}
  }

  async function checkLogin() {
    if (!token.value) return false
    try {
      const res = await getProfile()
      if (res.code === 0) {
        user.value = res.data
        ready.value = true
        return true
      }
    } catch {}
    token.value = ''
    user.value = null
    return false
  }

  async function loginByPassword(username, password) {
    const res = await loginByPasswordApi(username, password)
    if (res.code === 0) {
      saveSession(res.data)
      return { ok: true }
    }
    return { ok: false, message: res.message || '登录失败' }
  }

  async function register(username, password, nickname) {
    const res = await registerApi(username, password, nickname)
    if (res.code === 0) {
      saveSession(res.data)
      return { ok: true }
    }
    return { ok: false, message: res.message || '注册失败' }
  }

  async function updateProfile(nickname, avatar) {
    const res = await updateProfileApi(nickname, avatar)
    if (res.code === 0) {
      if (user.value) {
        if (nickname) user.value.nickname = nickname
        if (avatar) user.value.avatar = avatar
      }
      uni.setStorageSync('user', JSON.stringify(user.value))
      return true
    }
    return false
  }

  async function fetchProfile() {
    try {
      const res = await getProfile()
      if (res.code === 0) user.value = res.data
    } catch {}
  }

  function saveSession(data) {
    token.value = data.token
    user.value = data.user
    ready.value = true
    uni.setStorageSync('token', data.token)
    uni.setStorageSync('user', JSON.stringify(data.user))
  }

  function logout() {
    token.value = ''
    user.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('user')
  }

  return {
    token, user, ready, isLoggedIn,
    init, checkLogin, loginByPassword, register,
    updateProfile, fetchProfile, logout
  }
})
