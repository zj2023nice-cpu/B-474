<template>
  <div>
    <div class="header-actions">
      <el-button type="primary" @click="showAddDialog">新增用户</el-button>
    </div>

    <el-table :data="users" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="scope">
          <el-tag>{{ getRoleText(scope.row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-dropdown trigger="click" @command="(cmd) => handleAction(cmd, scope.row)">
            <el-button type="primary" size="small" plain>
              操作
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑资料</el-dropdown-item>
                <el-dropdown-item command="reset-password">重置密码</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="userDialogVisible" :title="userForm.id ? '编辑用户' : '新增用户'" width="400px">
      <el-form :model="userForm" label-width="80px" ref="userFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" />
        </el-form-item>
        <el-form-item label="角色" prop="role" :rules="[{ required: true, message: '请选择角色', trigger: 'change' }]">
          <el-select v-model="userForm.role" placeholder="选择角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUserSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialogVisible" title="重置密码" width="400px" @close="resetResetForm">
      <el-form :model="resetForm" label-width="100px" ref="resetFormRef">
        <el-form-item label="目标用户">
          <el-input :value="resetTargetUser ? `${resetTargetUser.name} (${resetTargetUser.username})` : ''" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword" :rules="resetPasswordRules">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword" :rules="resetConfirmRules">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sha256 } from '../utils/crypto'

const users = ref([])
const userDialogVisible = ref(false)
const userFormRef = ref(null)
const userForm = ref({})

const resetDialogVisible = ref(false)
const resetFormRef = ref(null)
const resetTargetUser = ref(null)
const resetForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const resetPasswordRules = [
  { required: true, message: '新密码不能为空', trigger: 'blur' },
  { min: 4, max: 100, message: '密码长度应在 4-100 个字符之间', trigger: 'blur' }
]

const resetConfirmRules = [
  { required: true, message: '请确认密码', trigger: 'blur' },
  { validator: validateConfirmPassword, trigger: 'blur' }
]

const fetchUsers = async () => {
  users.value = await request.get('/users')
}

const getRoleText = (role) => {
  const map = {
    'ADMIN': '管理员',
    'TEACHER': '教师',
    'STUDENT': '学生'
  }
  return map[role] || role
}

const showAddDialog = () => {
  userForm.value = { role: 'STUDENT' }
  userDialogVisible.value = true
}

const handleEdit = (row) => {
  userForm.value = { ...row }
  userDialogVisible.value = true
}

const handleUserSubmit = async () => {
  if (!userFormRef.value) return
  try {
    await userFormRef.value.validate()
  } catch (e) {
    return
  }

  if (userForm.value.id) {
    const { password, ...updateData } = userForm.value
    await request.put(`/users/${userForm.value.id}`, updateData)
    ElMessage.success('用户资料已更新')
  } else {
    if (!userForm.value.password) {
      ElMessage.error('请输入密码')
      return
    }
    const formData = { ...userForm.value }
    formData.password = await sha256(formData.password)
    await request.post('/users', formData)
    ElMessage.success('用户已创建')
  }
  userDialogVisible.value = false
  fetchUsers()
}

const handleAction = (command, row) => {
  switch (command) {
    case 'edit':
      handleEdit(row)
      break
    case 'reset-password':
      showResetPasswordDialog(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

const showResetPasswordDialog = (row) => {
  resetTargetUser.value = row
  resetResetForm()
  resetDialogVisible.value = true
}

const resetResetForm = () => {
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  if (resetFormRef.value) {
    resetFormRef.value.clearValidate()
  }
}

const handleResetPassword = async () => {
  if (!resetTargetUser.value) {
    ElMessage.error('未选择目标用户')
    return
  }

  if (!resetFormRef.value) return
  try {
    await resetFormRef.value.validate()
  } catch (e) {
    return
  }

  if (!resetForm.newPassword || !resetForm.newPassword.trim()) {
    ElMessage.error('新密码不能为空')
    return
  }

  if (resetForm.newPassword !== resetForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  try {
    const hashedPassword = await sha256(resetForm.newPassword)
    await request.put(`/users/${resetTargetUser.value.id}/reset-password`, {
      newPassword: hashedPassword,
      confirmPassword: hashedPassword
    })
    ElMessage.success(`密码重置成功！用户 ${resetTargetUser.value.name} 的新密码已生效`)
    resetDialogVisible.value = false
  } catch (error) {
    if (error.response?.status === 404) {
      fetchUsers()
    }
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除用户 ${row.name}？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/users/${row.id}`)
    fetchUsers()
    ElMessage.success('已删除')
  })
}

onMounted(fetchUsers)
</script>

<style scoped>
.header-actions {
  margin-bottom: 20px;
}
</style>
