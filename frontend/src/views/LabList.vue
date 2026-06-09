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

    <el-drawer v-model="drawerVisible" title="实验室详情" size="600px" :destroy-on-close="true">
      <div v-loading="detailLoading">
        <template v-if="detailData">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="名称">{{ detailData.name }}</el-descriptions-item>
            <el-descriptions-item label="楼宇">{{ detailData.building || '-' }}</el-descriptions-item>
            <el-descriptions-item label="房间号">{{ detailData.room || '-' }}</el-descriptions-item>
            <el-descriptions-item label="容量">{{ detailData.capacity != null ? detailData.capacity : '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ detailData.picName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detailData.picPhone || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">设备统计</el-divider>
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-value">{{ detailData.totalEquipment }}</div>
              <div class="stat-label">设备总数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value" style="color: #67c23a">{{ detailData.statusCounts?.NORMAL || 0 }}</div>
              <div class="stat-label">正常</div>
            </div>
            <div class="stat-card">
              <div class="stat-value" style="color: #e6a23c">{{ detailData.statusCounts?.BORROWED || 0 }}</div>
              <div class="stat-label">借用中</div>
            </div>
            <div class="stat-card">
              <div class="stat-value" style="color: #f56c6c">{{ detailData.statusCounts?.REPAIRING || 0 }}</div>
              <div class="stat-label">维修中</div>
            </div>
            <div class="stat-card">
              <div class="stat-value" style="color: #909399">{{ detailData.statusCounts?.SCRAPPED || 0 }}</div>
              <div class="stat-label">报废</div>
            </div>
          </div>

          <el-divider content-position="left">设备清单</el-divider>
          <div class="equipment-filter">
            <el-select v-model="equipFilter.status" placeholder="状态筛选" style="width: 120px" clearable @change="fetchEquipments" @clear="fetchEquipments">
              <el-option label="正常" value="NORMAL" />
              <el-option label="借用中" value="BORROWED" />
              <el-option label="维修中" value="REPAIRING" />
              <el-option label="报废" value="SCRAPPED" />
            </el-select>
          </div>
          <el-table :data="equipPageData.content" style="width: 100%" size="small">
            <el-table-column prop="code" label="编号" width="110" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="model" label="型号" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container" v-if="equipPagination.total > 0">
            <el-pagination
              v-model:current-page="equipPagination.currentPage"
              v-model:page-size="equipPagination.pageSize"
              :page-sizes="[5, 10, 20]"
              :total="equipPagination.total"
              layout="total, sizes, prev, pager, next"
              small
              @size-change="fetchEquipments"
              @current-change="fetchEquipments"
            />
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const dialogVisible = ref(false)
const form = ref({})

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const equipFilter = reactive({ status: '' })
const equipPageData = ref({
  content: [],
  totalPages: 0,
  totalElements: 0
})
const equipPagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

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

const fetchEquipments = async () => {
  if (!detailData.value) return
  const params = {
    page: equipPagination.currentPage,
    size: equipPagination.pageSize,
    labId: detailData.value.id
  }
  if (equipFilter.status) {
    params.status = equipFilter.status
  }
  const response = await request.get('/equipments', { params })
  equipPageData.value = response
  equipPagination.total = response.totalElements
}

const openDetail = async (row) => {
  drawerVisible.value = true
  detailLoading.value = true
  detailData.value = null
  equipFilter.status = ''
  equipPagination.currentPage = 1
  equipPagination.pageSize = 10
  try {
    const response = await request.get(`/labs/${row.id}/detail`)
    detailData.value = response
    fetchEquipments()
  } catch (e) {
    // handled in request.js
  } finally {
    detailLoading.value = false
  }
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

:deep(.el-drawer__body) {
  padding: 20px;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #303133;
}

.stats-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 80px;
  text-align: center;
  padding: 12px 8px;
  background: #f5f7fa;
  border-radius: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.equipment-filter {
  margin-bottom: 12px;
}
</style>
