<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <span>个人资料</span>
      </template>
      <el-descriptions :column="1" border v-if="!editing">
        <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ profile.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag>{{ getRoleText(profile.role) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-form v-if="editing" :model="form" label-width="100px" style="max-width: 400px">
        <el-form-item label="用户名">
          <el-input :model-value="profile.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="getRoleText(profile.role)" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </el-form-item>
      </el-form>

      <div v-if="!editing" style="margin-top: 16px">
        <el-button type="primary" @click="startEdit">编辑</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const profile = ref({})
const editing = ref(false)
const saving = ref(false)
const form = ref({ name: '', password: '', confirmPassword: '' })

const getRoleText = (role) => {
  const map = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[role] || role
}

const loadProfile = async () => {
  try {
    const data = await userStore.fetchProfile()
    profile.value = data
  } catch (e) {
    ElMessage.error('获取个人资料失败')
  }
}

const startEdit = () => {
  form.value = { name: profile.value.name || '', password: '', confirmPassword: '' }
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
  form.value = { name: '', password: '', confirmPassword: '' }
}

const handleSave = async () => {
  if (form.value.password && form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  saving.value = true
  try {
    const payload = { name: form.value.name }
    if (form.value.password) {
      payload.password = form.value.password
    }
    const data = await userStore.patchProfile(payload)
    profile.value = data
    editing.value = false
    ElMessage.success('更新成功')
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-container {
  max-width: 600px;
}
</style>
