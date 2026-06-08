<template>
  <div>
    <div class="header-actions">
      <el-button v-if="isAdmin" type="primary" @click="showAddDialog">新增设备</el-button>
      <el-input v-model="searchForm.name" placeholder="搜索名称" style="width: 150px; margin-left: 10px" clearable @clear="handleSearch" />
      <el-input v-model="searchForm.code" placeholder="搜索编号" style="width: 150px; margin-left: 10px" clearable @clear="handleSearch" />
      <el-select v-model="searchForm.status" placeholder="状态筛选" style="width: 120px; margin-left: 10px" clearable @clear="handleSearch">
        <el-option label="正常" value="NORMAL" />
        <el-option label="借用中" value="BORROWED" />
        <el-option label="维修中" value="REPAIRING" />
        <el-option label="报废" value="SCRAPPED" />
      </el-select>
      <el-select v-model="searchForm.labId" placeholder="实验室筛选" style="width: 150px; margin-left: 10px" clearable @clear="handleSearch">
        <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="lab.id" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      <el-button style="margin-left: 10px" @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pageData.content" style="width: 100%">
      <el-table-column prop="code" label="编号" width="120" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="model" label="型号" />
      <el-table-column prop="lab.name" label="所属实验室" />
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseDate" label="采购日期" />
      <el-table-column label="操作" v-if="isAdmin">
        <template #default="scope">
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增设备">
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="生产厂商">
          <el-input v-model="form.manufacturer" />
        </el-form-item>
        <el-form-item label="所属实验室">
          <el-select v-model="form.labId" placeholder="选择实验室">
            <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="lab.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" />
        </el-form-item>
        <el-form-item label="使用年限(年)">
          <el-input-number v-model="form.lifeSpan" />
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
import { ref, computed, reactive, onMounted, watch } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const labs = ref([])
const dialogVisible = ref(false)
const form = ref({})

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
  code: '',
  status: '',
  labId: null
})

const getStatusType = (status) => {
  if (status === 'NORMAL') return 'success'
  if (status === 'BORROWED') return 'warning'
  if (status === 'REPAIRING') return 'danger'
  return 'info'
}

const getStatusText = (status) => {
  if (status === 'NORMAL') return '正常'
  if (status === 'BORROWED') return '借用中'
  if (status === 'REPAIRING') return '维修中'
  if (status === 'SCRAPPED') return '报废'
  return status
}

const fetchEquipments = async () => {
  const params = {
    page: pagination.currentPage,
    size: pagination.pageSize
  }
  
  if (searchForm.name) {
    params.name = searchForm.name
  }
  if (searchForm.code) {
    params.code = searchForm.code
  }
  if (searchForm.status) {
    params.status = searchForm.status
  }
  if (searchForm.labId) {
    params.labId = searchForm.labId
  }
  
  const response = await request.get('/equipments', { params })
  pageData.value = response
  pagination.total = response.totalElements
}

const fetchLabs = async () => {
  const params = {
    page: 1,
    size: 1000
  }
  const response = await request.get('/labs', { params })
  labs.value = response.content
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchEquipments()
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.code = ''
  searchForm.status = ''
  searchForm.labId = null
  pagination.currentPage = 1
  fetchEquipments()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchEquipments()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchEquipments()
}

const showAddDialog = () => {
  form.value = { price: 0, lifeSpan: 5 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const payload = {
    ...form.value,
    lab: { id: form.value.labId }
  }
  await request.post('/equipments', payload)
  dialogVisible.value = false
  fetchEquipments()
  ElMessage.success('设备已添加')
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '警告', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }).then(async () => {
    await request.delete(`/equipments/${row.id}`)
    fetchEquipments()
    ElMessage.success('已删除')
  })
}

onMounted(() => {
  fetchLabs()
  fetchEquipments()
})
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
