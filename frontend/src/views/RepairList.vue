<template>
  <div>
    <div class="header-actions">
      <el-button v-if="canReport" type="primary" @click="showReportDialog">申请报修</el-button>
      <el-alert v-if="isStudent" type="info" :closable="false" show-icon class="student-tip">
        <template #title>学生账号仅可查看维修记录，如需报修请联系教师</template>
      </el-alert>
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
      <el-table-column prop="equipment.name" label="设备名称" min-width="120">
        <template #default="scope">
          {{ scope.row.equipment?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="equipment.code" label="设备编号" width="120">
        <template #default="scope">
          {{ scope.row.equipment?.code || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="故障描述" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="reporter.name" label="报修人" width="90">
        <template #default="scope">
          {{ scope.row.reporter?.name || scope.row.reporter?.username || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="reportDate" label="报修日期" width="160" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="repairConclusion" label="维修结论" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.repairConclusion || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="repairCompany" label="维修单位" width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.repairCompany || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="cost" label="维修费用" width="100">
        <template #default="scope">
          <span v-if="scope.row.cost !== null && scope.row.cost !== undefined">
            ¥{{ Number(scope.row.cost).toFixed(2) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="finishDate" label="完成日期" width="160">
        <template #default="scope">
          {{ scope.row.finishDate || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            text
            @click="showDetailDialog(scope.row)"
          >查看</el-button>
          <el-button
            v-if="canFinish(scope.row)"
            type="success"
            size="small"
            text
            @click="showFinishDialog(scope.row)"
          >完成维修</el-button>
          <el-button
            v-if="canCancel(scope.row)"
            type="danger"
            size="small"
            text
            @click="handleCancel(scope.row)"
          >取消</el-button>
          <el-button
            v-if="isAdmin && scope.row.status === 'FINISHED'"
            type="danger"
            size="small"
            text
            @click="handleDelete(scope.row)"
          >删除</el-button>
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

    <el-dialog v-model="reportDialogVisible" title="申请报修" width="500px">
      <el-form ref="reportFormRef" :model="reportForm" :rules="reportFormRules" label-width="100px">
        <el-form-item label="设备" prop="equipmentId">
          <el-select v-model="reportForm.equipmentId" placeholder="选择设备" filterable style="width: 100%">
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="`${eq.name} (${eq.code})`"
              :value="eq.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="description">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="请详细描述故障情况"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReportSubmit">提交报修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishDialogVisible" title="完成维修" width="500px">
      <el-form ref="finishFormRef" :model="finishForm" :rules="finishFormRules" label-width="100px">
        <el-form-item label="设备">
          <span>{{ currentRepair?.equipment?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="故障描述">
          <span>{{ currentRepair?.description || '-' }}</span>
        </el-form-item>
        <el-form-item label="维修结论" prop="repairConclusion">
          <el-input
            v-model="finishForm.repairConclusion"
            type="textarea"
            :rows="4"
            placeholder="请填写维修结论（必填）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="维修单位" prop="repairCompany">
          <el-input v-model="finishForm.repairCompany" placeholder="选填，维修单位名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="维修费用" prop="cost">
          <el-input-number v-model="finishForm.cost" :min="0" :precision="2" :step="10" style="width: 200px" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px">元，选填</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleFinishSubmit">确认完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="维修详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRepair">
        <el-descriptions-item label="维修单ID">{{ currentRepair.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRepair.status)">{{ getStatusText(currentRepair.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="设备名称" :span="2">
          {{ currentRepair.equipment?.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="设备编号" :span="2">
          {{ currentRepair.equipment?.code || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="报修人">{{ currentRepair.reporter?.name || currentRepair.reporter?.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修日期">{{ currentRepair.reportDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">
          {{ currentRepair.description || '-' }}
        </el-descriptions-item>
        <template v-if="currentRepair.status === 'FINISHED'">
          <el-descriptions-item label="维修结论" :span="2">
            {{ currentRepair.repairConclusion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="维修单位">
            {{ currentRepair.repairCompany || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="维修费用">
            <span v-if="currentRepair.cost !== null && currentRepair.cost !== undefined">
              ¥{{ Number(currentRepair.cost).toFixed(2) }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="完成日期" :span="2">
            {{ currentRepair.finishDate || '-' }}
          </el-descriptions-item>
        </template>
      </el-descriptions>
      <template #footer>
        <el-button v-if="canFinish(currentRepair)" type="success" @click="switchToFinish">去完成维修</el-button>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
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
const canReport = computed(() => isAdmin.value || isTeacher.value)
const isStudent = computed(() => userStore.role === 'STUDENT')

const equipments = ref([])
const reportDialogVisible = ref(false)
const reportFormRef = ref(null)
const reportForm = ref({
  equipmentId: null,
  description: ''
})

const reportFormRules = {
  equipmentId: [
    { required: true, message: '请选择设备', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请填写故障描述', trigger: 'blur' },
    { max: 1000, message: '故障描述长度不能超过 1000 个字符', trigger: 'blur' }
  ]
}

const finishDialogVisible = ref(false)
const finishFormRef = ref(null)
const currentRepair = ref(null)
const finishForm = ref({
  id: null,
  repairConclusion: '',
  repairCompany: '',
  cost: null
})

const finishFormRules = {
  repairConclusion: [
    { required: true, message: '请填写维修结论', trigger: 'blur' },
    { max: 1000, message: '维修结论长度不能超过 1000 个字符', trigger: 'blur' }
  ],
  repairCompany: [
    { max: 100, message: '维修单位名称长度不能超过 100 个字符', trigger: 'blur' }
  ]
}

const detailDialogVisible = ref(false)

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
  reportForm.value = {
    equipmentId: null,
    description: ''
  }
  reportDialogVisible.value = true
}

const handleReportSubmit = async () => {
  if (!reportFormRef.value) return
  await reportFormRef.value.validate()
  
  const payload = {
    equipment: { id: reportForm.value.equipmentId },
    description: reportForm.value.description,
    reporter: { id: userStore.user.id }
  }
  await request.post('/repairs', payload)
  reportDialogVisible.value = false
  fetchRepairs()
  ElMessage.success('已报修')
}

const canFinish = (row) => {
  if (!row) return false
  if (!isAdmin.value) return false
  return row.status === 'REPORTED' || row.status === 'IN_PROGRESS'
}

const canCancel = (row) => {
  if (!row) return false
  if (row.status !== 'REPORTED') return false
  if (isAdmin.value) return true
  if (row.reporter?.id === userStore.user?.id) return true
  return false
}

const showFinishDialog = (row) => {
  currentRepair.value = row
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

const showDetailDialog = async (row) => {
  try {
    const response = await request.get(`/repairs/${row.id}`)
    currentRepair.value = response
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const switchToFinish = () => {
  detailDialogVisible.value = false
  showFinishDialog(currentRepair.value)
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确认取消该报修？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/repairs/${row.id}`)
    fetchRepairs()
    ElMessage.success('已取消')
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该维修记录？删除后不可恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/repairs/${row.id}`)
    fetchRepairs()
    ElMessage.success('删除成功')
  })
}

const getStatusText = (status) => {
  if (status === 'REPORTED') return '已上报'
  if (status === 'IN_PROGRESS') return '维修中'
  if (status === 'FINISHED') return '已完成'
  return status || '-'
}

const getStatusType = (status) => {
  if (status === 'FINISHED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'REPORTED') return 'danger'
  return 'info'
}

onMounted(fetchRepairs)
</script>

<style scoped>
.header-actions {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.student-tip {
  flex: 1;
  min-width: 280px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
