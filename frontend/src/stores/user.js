import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

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

  return { user, token, isLoggedIn, role, login, logout }
})
