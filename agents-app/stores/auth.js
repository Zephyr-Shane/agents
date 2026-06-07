import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, phoneLoginApi, getProfile } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const user = ref(null)
  const ready = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  /** 是否已绑定手机号（完整账号） */
  const hasPhone = computed(() => !!user.value?.phone)

  function init() {
    try {
      const t = uni.getStorageSync('token')
      if (t) token.value = t
      const u = uni.getStorageSync('user')
      if (u) user.value = JSON.parse(u)
    } catch {}
  }

  async function autoLogin() {
    if (token.value) {
      try {
        const res = await getProfile()
        if (res.code === 0) {
          user.value = res.data
          ready.value = true
          return
        }
      } catch {}
      token.value = ''
      user.value = null
    }
    try {
      const code = await getWxLoginCode()
      if (code) {
        const ok = await doLogin(code)
        if (!ok) {
          uni.showToast({ title: '登录失败，请检查后端是否运行', icon: 'none', duration: 3000 })
        }
      }
    } catch (e) {
      const msg = e?.errMsg || e?.message || '网络错误'
      console.error('静默登录失败', msg)
      uni.showToast({ title: '连接后端失败: ' + msg, icon: 'none', duration: 3000 })
    }
    ready.value = true
  }

  function getWxLoginCode() {
    return new Promise((resolve, reject) => {
      uni.login({
        success: (res) => {
          if (res.code) resolve(res.code)
          else reject(new Error('uni.login 未返回 code'))
        },
        fail: (err) => {
          if (err.errMsg && err.errMsg.includes('fail')) {
            resolve('h5_mock_' + Date.now())
          } else {
            reject(err)
          }
        }
      })
    })
  }

  async function doLogin(code) {
    const res = await loginApi(code)
    if (res.code === 0) {
      token.value = res.data.token
      user.value = res.data.user
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('user', JSON.stringify(res.data.user))
      return true
    }
    return false
  }

  async function reLogin() {
    token.value = ''
    user.value = null
    const code = await getWxLoginCode()
    await doLogin(code)
  }

  /**
   * 微信小程序手机号一键登录
   * 需要用户点击 button[open-type="getPhoneNumber"] 授权后调用
   * @param {Object} params
   * @param {string} params.loginCode - wx.login() 获取的 code
   * @param {string} params.phoneCode - wx.getPhoneNumber() 回调的 code
   * @param {string} [params.nickname] - 用户昵称（可选）
   * @param {string} [params.avatarUrl] - 头像 URL（可选）
   * @returns {Promise<boolean>} 是否登录成功
   */
  async function phoneLogin({ loginCode, phoneCode, nickname, avatarUrl }) {
    const res = await phoneLoginApi({
      loginCode,
      phoneCode,
      nickname: nickname || '',
      avatarUrl: avatarUrl || ''
    })
    if (res.code === 0) {
      token.value = res.data.token
      user.value = res.data.user
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('user', JSON.stringify(res.data.user))
      uni.showToast({ title: '登录成功', icon: 'success' })
      return true
    }
    uni.showToast({ title: '登录失败: ' + (res.message || '未知错误'), icon: 'none', duration: 3000 })
    return false
  }

  async function fetchProfile() {
    try {
      const res = await getProfile()
      if (res.code === 0) user.value = res.data
    } catch {}
  }

  function logout() {
    token.value = ''
    user.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('user')
  }

  return {
    token, user, ready, isLoggedIn, hasPhone,
    init, autoLogin, reLogin, phoneLogin, fetchProfile, logout
  }
})
