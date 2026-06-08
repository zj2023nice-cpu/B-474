<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>实验室设备管理系统</span>
        </div>
      </template>
      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import request from '../api/request'
import { User, Lock } from '@element-plus/icons-vue'
import { sha256 } from '../utils/crypto'
import { ElMessage, ElLoading } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const form = ref({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  const loading = ElLoading.service({
    lock: true,
    text: '登录中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    const hashedPassword = await sha256(form.value.password)
    const loginData = {
      username: form.value.username,
      password: hashedPassword
    }
    const res = await request.post('/login', loginData)
    
    if (res && res.token) {
      userStore.login(res)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error('登录失败，服务器返回数据格式错误')
    }
  } catch (e) {
    console.error('Login error:', e)
  } finally {
    loading.close()
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #2d3a4b;
}
.login-card {
  width: 400px;
}
.card-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
}
</style>
