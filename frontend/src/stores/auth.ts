import { defineStore } from 'pinia'
import { reactive } from 'vue'
import { login, getCurrentUser, type UserInfo } from '@/api/auth'
import { http } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const state = reactive({
    token: localStorage.getItem('token') || '',
    user: null as UserInfo | null,
    loaded: false,
  })

  async function init() {
    if (state.token) {
      http.defaults.headers.common['Authorization'] = 'Bearer ' + state.token
      try {
        const res = await getCurrentUser()
        state.user = res.data.data
      } catch {
        state.token = ''
        localStorage.removeItem('token')
        delete http.defaults.headers.common['Authorization']
      }
    }
    state.loaded = true
  }

  async function doLogin(username: string, password: string) {
    const res = await login({ username, password })
    const data = res.data.data
    state.token = data.token
    state.user = data.user
    localStorage.setItem('token', data.token)
    http.defaults.headers.common['Authorization'] = 'Bearer ' + data.token
  }

  function logout() {
    state.token = ''
    state.user = null
    localStorage.removeItem('token')
    delete http.defaults.headers.common['Authorization']
  }

  function isLoggedIn(): boolean {
    return !!state.token && !!state.user
  }

  function hasRole(role: string): boolean {
    return state.user?.roles?.includes(role) ?? false
  }

  return { state, init, doLogin, logout, isLoggedIn, hasRole }
})
