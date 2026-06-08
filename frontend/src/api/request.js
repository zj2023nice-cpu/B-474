import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 5000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.code !== undefined && res.code !== 200) {
      const errorMessage = res.message || '请求失败'
      ElMessage.error(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }
    
    return res.data
  },
  error => {
    let errorMessage = '请求失败'
    
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      
      if (data && typeof data === 'object' && data.message) {
        errorMessage = data.message
      } else if (typeof data === 'string') {
        errorMessage = data
      }
      
      if (status === 401) {
        const isLoginRequest = error.config?.url?.includes('/login')
        if (isLoginRequest) {
          ElMessage.error(errorMessage)
        } else {
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.href = '/login'
        }
        return Promise.reject(new Error(errorMessage))
      }
      
      if (status === 403) {
        errorMessage = errorMessage || '权限不足，无法执行此操作'
      } else if (status === 404) {
        errorMessage = errorMessage || '请求的资源不存在'
      } else if (status === 500) {
        errorMessage = errorMessage || '服务器内部错误'
      } else if (status === 400) {
        errorMessage = errorMessage || '请求参数错误'
      }
      
      ElMessage.error(errorMessage)
    } else {
      if (error.message.includes('timeout')) {
        errorMessage = '请求超时，请稍后重试'
      } else if (error.message.includes('Network Error')) {
        errorMessage = '网络错误，请检查网络连接'
      } else {
        errorMessage = error.message || '请求失败'
      }
      ElMessage.error(errorMessage)
    }
    
    return Promise.reject(new Error(errorMessage))
  }
)

export default service
