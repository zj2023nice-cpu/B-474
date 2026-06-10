<template>
  <div>
    <div class="header-actions">
      <el-button v-if="isAdmin" type="primary" @click="showAddDialog">新增实验室</el-button>
      <el-input v-model="searchForm.name" placeholder="搜索名称" style="width: 150px; margin-left: 10px" clearable @clear="handleSearch" />
      <el-input v-model="searchForm.building" placeholder="搜索楼宇" style="width: 150px; margin-left: 10px" clearable @clear="handleSearch" />
      <el-input v-model="searchForm.picName" placeholder="搜索负责人" style="width: 150px; margin-left: 10px" clearable @clear="handleSearch" />
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      <el-button style="margin-left: 10px" @click="resetSearch">重置</el-button>
    </div>
    
    <el-table :data="pageData.content" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="building" label="楼宇" />
      <el-table-column prop="room" label="房间号" />
      <el-table-column prop="picName" label="负责人" />
      <el-table-column prop="picPhone" label="电话" />
      <el-table-column prop="capacity" label="容量" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" size="small" @click="openDetail(scope.row)">详情</el-button>
          <el-button v-if="isAdmin" type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button v-if="isAdmin" type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑实验室' : '新增实验室'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="楼宇">
          <el-input v-model="form.building" />
        </el-form-item>
        <el-form-item label="房间号">
          <el-input v-model="form.room" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.picName" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.picPhone" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <LabDetailDrawer v-model="drawerVisible" :lab-id="selectedLabId" />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import LabDetailDrawer from '../components/LabDetailDrawer.vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const dialogVisible = ref(false)
const form = ref({})

const drawerVisible = ref(false)
const selectedLabId = ref(null)

const pageData = ref({
  content: [],
  totalPages: 0,
  totalElements: 0,
  currentPage: 1,
  pageSize: 10,
  hasNext: false,
  hasPrevious: false
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const searchForm = reactive({
  name: '',
  building: '',
  picName: ''
})

const fetchLabs = async () => {
  const params = {
    page: pagination.currentPage,
    size: pagination.pageSize
  }
  
  if (searchForm.name) {
    params.name = searchForm.name
  }
  if (searchForm.building) {
    params.building = searchForm.building
  }
  if (searchForm.picName) {
    params.picName = searchForm.picName
  }
  
  const response = await request.get('/labs', { params })
  pageData.value = response
  pagination.total = response.totalElements
}

const openDetail = (row) => {
  selectedLabId.value = row.id
  drawerVisible.value = true
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchLabs()
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.building = ''
  searchForm.picName = ''
  pagination.currentPage = 1
  fetchLabs()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchLabs()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchLabs()
}

const showAddDialog = () => {
  form.value = { capacity: 0 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.value.id) {
    await request.put(`/labs/${form.value.id}`, form.value)
    ElMessage.success('实验室已更新')
  } else {
    await request.post('/labs', form.value)
    ElMessage.success('实验室已添加')
  }
  dialogVisible.value = false
  fetchLabs()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/labs/${row.id}`)
    fetchLabs()
    ElMessage.success('已删除')
  })
}

onMounted(fetchLabs)
</script>

<style scoped>
.header-actions {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
