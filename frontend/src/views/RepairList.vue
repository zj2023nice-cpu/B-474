<template>
  <div>
    <div class="header-actions">
      <el-button v-if="isTeacher" type="primary" @click="showReportDialog">申请报修</el-button>
      <el-select v-model="searchForm.status" placeholder="状态筛选" style="width: 120px; margin-left: 10px" clearable @clear="handleSearch">
        <el-option label="已上报" value="REPORTED" />
        <el-option label="维修中" value="IN_PROGRESS" />
        <el-option label="已完成" value="FINISHED" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      <el-button style="margin-left: 10px" @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pageData.content" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="equipment.name" label="设备" />
      <el-table-column prop="description" label="故障描述" />
      <el-table-column prop="reportDate" label="报修日期" />
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'FINISHED' ? 'success' : 'danger'">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="repairConclusion" label="维修结论" />
      <el-table-column prop="finishDate" label="完成日期" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button
            v-if="isAdmin && scope.row.status !== 'FINISHED'"
            type="success"
            size="small"
            @click="showFinishDialog(scope.row)"
          >完成维修</el-button>
          <el-button
            v-if="scope.row.status === 'REPORTED' && (isAdmin || scope.row.reporter.id === userStore.user.id)"
            type="danger"
            size="small"
            @click="handleCancel(scope.row)"
          >取消报修</el-button>
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

    <el-dialog v-model="dialogVisible" title="申请报修">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备">
          <el-select v-model="form.equipmentId" placeholder="选择设备" filterable>
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="`${eq.name} (${eq.code})`"
              :value="eq.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishDialogVisible" title="完成维修" width="480px">
      <el-form ref="finishFormRef" :model="finishForm" :rules="finishFormRules" label-width="100px">
        <el-form-item label="维修结论" prop="repairConclusion">
          <el-input
            v-model="finishForm.repairConclusion"
            type="textarea"
            :rows="3"
            placeholder="请填写维修结论"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="维修单位" prop="repairCompany">
          <el-input v-model="finishForm.repairCompany" placeholder="选填" maxlength="100" />
        </el-form-item>
        <el-form-item label="维修费用" prop="cost">
          <el-input-number v-model="finishForm.cost" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleFinishSubmit">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isTeacher = computed(() => userStore.role === 'TEACHER')

const equipments = ref([])
const dialogVisible = ref(false)
const form = ref({})

const finishDialogVisible = ref(false)
const finishFormRef = ref(null)
const finishForm = ref({
  id: null,
  repairConclusion: '',
  repairCompany: '',
  cost: null
})

const finishFormRules = {
  repairConclusion: [
    { required: true, message: '请填写维修结论', trigger: 'blur' }
  ]
}

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
  status: ''
})

const fetchRepairs = async () => {
  const params = {
    page: pagination.currentPage,
    size: pagination.pageSize
  }
  
  if (searchForm.status) {
    params.status = searchForm.status
  }
  
  const response = await request.get('/repairs', { params })
  pageData.value = response
  pagination.total = response.totalElements
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchRepairs()
}

const resetSearch = () => {
  searchForm.status = ''
  pagination.currentPage = 1
  fetchRepairs()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchRepairs()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchRepairs()
}

const showReportDialog = async () => {
  const params = {
    page: 1,
    size: 1000
  }
  const response = await request.get('/equipments', { params })
  equipments.value = response.content
  form.value = {}
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const payload = {
    equipment: { id: form.value.equipmentId },
    description: form.value.description,
    reporter: { id: userStore.user.id }
  }
  await request.post('/repairs', payload)
  dialogVisible.value = false
  fetchRepairs()
  ElMessage.success('已报修')
}

const showFinishDialog = (row) => {
  finishForm.value = {
    id: row.id,
    repairConclusion: '',
    repairCompany: row.repairCompany || '',
    cost: row.cost ?? null
  }
  finishDialogVisible.value = true
}

const handleFinishSubmit = async () => {
  if (!finishFormRef.value) return
  await finishFormRef.value.validate()
  
  const payload = {
    repairConclusion: finishForm.value.repairConclusion,
    repairCompany: finishForm.value.repairCompany || null,
    cost: finishForm.value.cost ?? null
  }
  await request.put(`/repairs/${finishForm.value.id}/finish`, payload)
  finishDialogVisible.value = false
  fetchRepairs()
  ElMessage.success('维修已完成')
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确认取消报修？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/repairs/${row.id}`)
    fetchRepairs()
    ElMessage.success('已取消')
  })
}

const getStatusText = (status) => {
  if (status === 'REPORTED') return '已上报'
  if (status === 'IN_PROGRESS') return '维修中'
  if (status === 'FINISHED') return '已完成'
  return status
}

onMounted(fetchRepairs)
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
