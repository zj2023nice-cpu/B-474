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
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" v-if="!form.id">
          <el-input v-model="form.password" type="password" />
        </el-form-item>
        <el-form-item label="密码" v-else>
          <el-input v-model="form.password" type="password" placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sha256 } from '../utils/crypto'

const users = ref([])
const dialogVisible = ref(false)
const form = ref({})

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
  form.value = { role: 'STUDENT' }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.value = { ...row, password: '' }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.value.id) {
    if (form.value.password) {
      form.value.password = await sha256(form.value.password)
    } else {
      delete form.value.password
    }
    await request.put(`/users/${form.value.id}`, form.value)
    ElMessage.success('用户已更新')
  } else {
    if (!form.value.password) {
      ElMessage.error('请输入密码')
      return
    }
    form.value.password = await sha256(form.value.password)
    await request.post('/users', form.value)
    ElMessage.success('用户已创建')
  }
  dialogVisible.value = false
  fetchUsers()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '警告', {
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