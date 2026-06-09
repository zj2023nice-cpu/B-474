import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getProfile, updateProfile } from '../api/profile'
import { sha256 } from '../utils/crypto'

export const useUserStore = defineStore('user', () => {
  const user = ref(JSON.parse(localStorage.getItem('user')) || null)
  const token = ref(localStorage.getItem('token') || null)

  const isLoggedIn = computed(() => !!user.value && !!token.value)
  const role = computed(() => user.value?.role)

  function login(userData) {
    user.value = {
      id: userData.id,
      username: userData.username,
      role: userData.role,
      name: userData.name
    }
    token.value = userData.token
    localStorage.setItem('user', JSON.stringify(user.value))
    localStorage.setItem('token', token.value)
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  async function fetchProfile() {
    const data = await getProfile()
    user.value = {
      id: data.id,
      username: data.username,
      role: data.role,
      name: data.name
    }
    localStorage.setItem('user', JSON.stringify(user.value))
    return data
  }

  async function patchProfile({ name, password }) {
    const payload = {}
    if (name !== undefined) payload.name = name
    if (password) payload.password = await sha256(password)
    const data = await updateProfile(payload)
    user.value = {
      id: data.id,
      username: data.username,
      role: data.role,
      name: data.name
    }
    localStorage.setItem('user', JSON.stringify(user.value))
    return data
  }

  return { user, token, isLoggedIn, role, login, logout, fetchProfile, patchProfile }
})
