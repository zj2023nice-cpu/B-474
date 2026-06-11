<template>
  <div>
    <div class="header-actions">
      <el-button v-if="canReport" type="primary" @click="showApplyDialog">申请报修</el-button>
      <el-alert v-if="isStudent" type="info" :closable="false" show-icon class="student-tip">
        <template #title>学生账号仅可查看报修记录，如需申请请联系教师</template>
      </el-alert>
      <el-select v-model="searchForm.status" placeholder="状态筛选" style="width: 130px; margin-left: 10px" clearable @change="handleSearch">
        <el-option label="已上报" :value="RepairStatus.REPORTED" />
        <el-option label="维修中" :value="RepairStatus.IN_PROGRESS" />
        <el-option label="已完成" :value="RepairStatus.FINISHED" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      <el-button style="margin-left: 10px" @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pageData.content" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="equipment.name" label="设备" min-width="100">
        <template #default="scope">{{ scope.row.equipment?.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="reporter.name" label="报修人" width="90">
        <template #default="scope">{{ scope.row.reporter?.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="faultDescription" label="故障描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="reportTime" label="报修时间" width="160" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="getRepairStatusType(scope.row.status)">
            {{ getRepairStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="repairResult" label="维修结果" min-width="150" show-overflow-tooltip />
      <el-table-column prop="repairTime" label="维修时间" width="160">
        <template #default="scope">{{ scope.row.repairTime || '-' }}</template>
      </el-table-column>
      <el-table-column v-if="canReport" label="操作" width="200" fixed="right">
        <template #default="scope">
          <div v-if="(scope.row.status === RepairStatus.REPORTED || scope.row.status === RepairStatus.IN_PROGRESS) && isAdmin">
            <el-tooltip 
              v-if="!getRepairOperationAllowed(scope.row, BusinessOperation.FINISH_REPAIR)"
              :content="getRepairOperationDisabledReason(scope.row, BusinessOperation.FINISH_REPAIR)"
              placement="top"
            >
              <el-button 
                type="success" 
                size="small" 
                disabled
              >完成维修</el-button>
            </el-tooltip>
            <el-button 
              v-else
              type="success" 
              size="small" 
              :loading="finishing && scope.row.id === currentActionId"
              :disabled="finishing"
              @click="handleFinish(scope.row)"
            >完成维修</el-button>
          </div>
          <div v-if="scope.row.status === RepairStatus.REPORTED && (isAdmin || scope.row.reporter?.id === userStore.user.id)">
            <el-tooltip 
              v-if="!getRepairOperationAllowed(scope.row, BusinessOperation.CANCEL_REPAIR)"
              :content="getRepairOperationDisabledReason(scope.row, BusinessOperation.CANCEL_REPAIR)"
              placement="top"
            >
              <el-button 
                type="warning" 
                size="small" 
                disabled
              >取消</el-button>
            </el-tooltip>
            <el-button 
              v-else
              type="warning" 
              size="small" 
              :loading="cancelling && scope.row.id === currentActionId"
              :disabled="cancelling"
              @click="handleCancel(scope.row)"
            >取消</el-button>
          </div>
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

    <el-dialog v-model="dialogVisible" title="申请报修" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备">
          <el-select v-model="form.equipmentId" placeholder="选择设备" filterable>
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="`${eq.name} (${eq.code})`"
              :value="eq.id"
              :disabled="!canRepairEquipment(eq) || eq.hasActiveRepair"
            >
              <span>{{ eq.name }} ({{ eq.code }})</span>
              <el-tag 
                v-if="!canRepairEquipment(eq)" 
                size="small" 
                type="info"
                style="margin-left: 8px"
              >
                {{ getEquipmentDisabledReason(eq, BusinessOperation.REPORT_REPAIR) }}
              </el-tag>
              <el-tag 
                v-else-if="eq.hasActiveRepair" 
                size="small" 
                type="warning"
                style="margin-left: 8px"
              >
                存在未完成维修
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="form.faultDescription" type="textarea" placeholder="请详细描述故障情况" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="其他需要说明的情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="!form.equipmentId || !form.faultDescription">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finishDialogVisible" title="完成维修" width="500px">
      <el-form :model="finishForm" label-width="100px">
        <el-form-item label="维修结果">
          <el-input v-model="finishForm.repairResult" type="textarea" placeholder="请描述维修结果" />
        </el-form-item>
        <el-form-item label="维修备注">
          <el-input v-model="finishForm.remark" type="textarea" placeholder="维修备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFinishSubmit" :disabled="!finishForm.repairResult">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  RepairStatus,
  BusinessOperation,
  getRepairStatusText,
  getRepairStatusType,
  canRepairEquipment,
  getEquipmentDisabledReason
} from '../constants/statusConstants'
import { checkRepairOperation } from '../api/stateConstraint'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isTeacher = computed(() => userStore.role === 'TEACHER')
const canReport = computed(() => isAdmin.value || isTeacher.value)
const isStudent = computed(() => userStore.role === 'STUDENT')

const equipments = ref([])
const dialogVisible = ref(false)
const form = ref({})
const loading = ref(false)

const finishDialogVisible = ref(false)
const finishForm = ref({})
const currentFinishId = ref(null)
const finishing = ref(false)
const cancelling = ref(false)
const currentActionId = ref(null)

const repairOperationCache = ref(new Map())

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

function getRepairOperationAllowed(repair, operation) {
  const cacheKey = `${repair.id}_${operation}`
  if (repairOperationCache.value.has(cacheKey)) {
    return repairOperationCache.value.get(cacheKey).allowed
  }
  if (operation === BusinessOperation.FINISH_REPAIR) {
    return repair.status === RepairStatus.REPORTED || repair.status === RepairStatus.IN_PROGRESS
  }
  if (operation === BusinessOperation.CANCEL_REPAIR) {
    return repair.status === RepairStatus.REPORTED
  }
  return true
}

function getRepairOperationDisabledReason(repair, operation) {
  const cacheKey = `${repair.id}_${operation}`
  if (repairOperationCache.value.has(cacheKey)) {
    const result = repairOperationCache.value.get(cacheKey)
    return result.errorMessage || '当前状态不允许此操作'
  }
  if (operation === BusinessOperation.FINISH_REPAIR && 
      repair.status !== RepairStatus.REPORTED && 
      repair.status !== RepairStatus.IN_PROGRESS) {
    return '仅已上报或维修中状态的维修单可以完成'
  }
  if (operation === BusinessOperation.CANCEL_REPAIR && repair.status !== RepairStatus.REPORTED) {
    return '仅已上报状态的维修单可以取消'
  }
  return '当前状态不允许此操作'
}

async function prefetchRepairOperations(repairs) {
  const operations = [
    BusinessOperation.FINISH_REPAIR,
    BusinessOperation.CANCEL_REPAIR
  ]
  
  for (const repair of repairs) {
    for (const op of operations) {
      const cacheKey = `${repair.id}_${op}`
      if (!repairOperationCache.value.has(cacheKey)) {
        try {
          const result = await checkRepairOperation(repair.id, op)
          repairOperationCache.value.set(cacheKey, result)
        } catch (e) {
          console.warn(`Failed to check repair operation ${op} for repair ${repair.id}:`, e)
        }
      }
    }
  }
}

const fetchRepairs = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize
    }

    if (isTeacher.value) {
      params.userId = userStore.user.id
    }

    if (searchForm.status) {
      params.status = searchForm.status
    }

    const response = await request.get('/repairs', { params })
    pageData.value = response
    pagination.total = response.totalElements
    
    if (response.content && response.content.length > 0 && canReport.value) {
      prefetchRepairOperations(response.content)
    }
  } finally {
    loading.value = false
  }
}

const fetchRepairsWithPageFallback = async () => {
  let maxAttempts = 5
  let attempts = 0

  while (attempts < maxAttempts) {
    attempts++
    await fetchRepairs()

    const currentContent = pageData.value.content
    const totalPages = pageData.value.totalPages
    const contentEmpty = !currentContent || currentContent.length === 0
    const currentPage = pagination.currentPage

    if (!contentEmpty) {
      break
    }

    if (totalPages <= 0) {
      if (currentPage !== 1) {
        pagination.currentPage = 1
      } else {
        break
      }
    } else if (currentPage > totalPages) {
      pagination.currentPage = totalPages
    } else if (currentPage > 1) {
      pagination.currentPage = currentPage - 1
    } else {
      break
    }
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  repairOperationCache.value.clear()
  fetchRepairs()
}

const resetSearch = () => {
  searchForm.status = ''
  pagination.currentPage = 1
  repairOperationCache.value.clear()
  fetchRepairs()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  repairOperationCache.value.clear()
  fetchRepairs()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchRepairs()
}

const showApplyDialog = async () => {
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
  if (!form.value.equipmentId || !form.value.faultDescription) {
    ElMessage.error('请填写完整信息')
    return
  }

  const selectedEquipment = equipments.value.find(eq => eq.id === form.value.equipmentId)
  if (!canRepairEquipment(selectedEquipment)) {
    ElMessage.error(getEquipmentDisabledReason(selectedEquipment, BusinessOperation.REPORT_REPAIR))
    return
  }

  if (selectedEquipment.hasActiveRepair) {
    ElMessage.error('该设备存在未完成的维修记录，请先完成或取消现有维修单')
    return
  }

  const payload = {
    equipment: { id: form.value.equipmentId },
    reporter: { id: userStore.user.id },
    faultDescription: form.value.faultDescription,
    remark: form.value.remark,
    reportTime: new Date().toISOString().replace('T', ' ').split('.')[0]
  }

  try {
    await request.post('/repairs', payload)
    dialogVisible.value = false
    repairOperationCache.value.clear()
    fetchRepairs()
    ElMessage.success('报修已提交')
  } catch (e) {
    console.error(e)
  }
}

const handleFinish = async (row) => {
  if (finishing.value) return
  
  const checkResult = await checkRepairOperation(row.id, BusinessOperation.FINISH_REPAIR)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法完成维修')
    repairOperationCache.value.set(`${row.id}_${BusinessOperation.FINISH_REPAIR}`, checkResult)
    fetchRepairsWithPageFallback()
    return
  }
  
  currentFinishId.value = row.id
  finishForm.value = {
    repairResult: '',
    remark: ''
  }
  finishDialogVisible.value = true
}

const handleFinishSubmit = async () => {
  if (!finishForm.value.repairResult) {
    ElMessage.error('请填写维修结果')
    return
  }

  const checkResult = await checkRepairOperation(currentFinishId.value, BusinessOperation.FINISH_REPAIR)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法完成维修')
    repairOperationCache.value.set(`${currentFinishId.value}_${BusinessOperation.FINISH_REPAIR}`, checkResult)
    fetchRepairsWithPageFallback()
    finishDialogVisible.value = false
    return
  }

  currentActionId.value = currentFinishId.value
  finishing.value = true
  try {
    const payload = {
      repairResult: finishForm.value.repairResult,
      remark: finishForm.value.remark
    }
    const result = await request.put(`/repairs/${currentFinishId.value}/finish`, payload)

    const idx = pageData.value.content.findIndex(item => item.id === currentFinishId.value)
    if (idx !== -1 && result) {
      pageData.value.content.splice(idx, 1, result)
    }

    finishDialogVisible.value = false
    repairOperationCache.value.clear()
    await fetchRepairsWithPageFallback()
    ElMessage.success('维修已完成')
  } finally {
    finishing.value = false
    currentActionId.value = null
  }
}

const handleCancel = (row) => {
  if (cancelling.value) return
  
  checkRepairOperation(row.id, BusinessOperation.CANCEL_REPAIR).then(checkResult => {
    if (!checkResult.allowed) {
      ElMessage.warning(checkResult.errorMessage || '当前状态无法取消')
      repairOperationCache.value.set(`${row.id}_${BusinessOperation.CANCEL_REPAIR}`, checkResult)
      fetchRepairsWithPageFallback()
      return
    }

    ElMessageBox.confirm('确认取消报修？取消后无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      currentActionId.value = row.id
      cancelling.value = true
      try {
        const result = await request.delete(`/repairs/${row.id}`)

        const idx = pageData.value.content.findIndex(item => item.id === row.id)
        if (idx !== -1) {
          pageData.value.content.splice(idx, 1)
        }

        repairOperationCache.value.clear()
        await fetchRepairsWithPageFallback()
        ElMessage.success('已取消')
      } finally {
        cancelling.value = false
        currentActionId.value = null
      }
    }).catch(() => {
      cancelling.value = false
      currentActionId.value = null
    })
  })
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
