<template>
  <div>
    <div class="header-actions">
      <el-button v-if="canReport" type="primary" @click="showReportDialog" :loading="reportEquipmentsReq.loading.value">申请报修</el-button>
      <el-alert v-if="isStudent" type="info" :closable="false" show-icon class="student-tip">
        <template #title>学生账号仅可查看报修记录，如需报修请联系教师</template>
      </el-alert>
      <el-select v-model="listPage.searchForm.status" placeholder="状态筛选" style="width: 120px; margin-left: 10px" clearable @clear="listPage.handleSearch" @change="listPage.handleSearch">
        <el-option label="已上报" :value="RepairStatus.REPORTED" />
        <el-option label="维修中" :value="RepairStatus.IN_PROGRESS" />      
        <el-option label="已完成" :value="RepairStatus.FINISHED" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="listPage.handleSearch" :loading="listPage.loading.value">搜索</el-button>
      <el-button style="margin-left: 10px" @click="listPage.resetSearch" :disabled="listPage.loading.value">重置</el-button>
    </div>

    <el-table :data="listPage.pageData.content" style="width: 100%" v-loading="listPage.loading.value">     
      <el-table-column prop="id" label="ID" width="60" />       
      <el-table-column prop="equipment.name" label="设备名称" min-width="100">
        <template #default="scope">
          {{ scope.row.equipment?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="equipment.code" label="设备编号" width="110">
        <template #default="scope">
          {{ scope.row.equipment?.code || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="故障描述" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="reporter.name" label="报修人" width="80">
        <template #default="scope">
          {{ scope.row.reporter?.name || scope.row.reporter?.username || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="reportDate" label="报修日期" width="160" />
      <el-table-column prop="status" label="状态" width="90">  
        <template #default="scope">
          <el-tag :type="getRepairStatusType(scope.row.status)">{{ getRepairStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="repairConclusion" label="维修结论" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.repairConclusion || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="repairCompany" label="维修单位" width="110" show-overflow-tooltip>
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
      <el-table-column v-if="canReport" label="操作" width="260" fixed="right">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            text
            @click="showDetailDialog(scope.row)"
          >查看</el-button>
          <template v-if="(scope.row.status === RepairStatus.REPORTED || scope.row.status === RepairStatus.IN_PROGRESS) && isAdmin">
            <el-tooltip 
              v-if="!getRepairOperationAllowed(scope.row, BusinessOperation.FINISH_REPAIR)"
              :content="getRepairOperationDisabledReason(scope.row, BusinessOperation.FINISH_REPAIR)"
              placement="top"
            >
              <el-button
                type="success"
                size="small"
                text
                disabled
              >完成维修</el-button>
            </el-tooltip>
            <el-button
              v-else
              type="success"
              size="small"
              text
              :loading="finishMutation.loading.value && currentActionId.value === scope.row.id"
              :disabled="finishMutation.locked.value"
              @click="showFinishDialog(scope.row)"
            >完成维修</el-button>
          </template>
          <template v-if="scope.row.status === RepairStatus.REPORTED && (isAdmin || scope.row.reporter?.id === userStore.user?.id)">
            <el-tooltip 
              v-if="!getRepairOperationAllowed(scope.row, BusinessOperation.CANCEL_REPAIR)"
              :content="getRepairOperationDisabledReason(scope.row, BusinessOperation.CANCEL_REPAIR)"
              placement="top"
            >
              <el-button
                type="danger"
                size="small"
                text
                disabled
              >取消</el-button>
            </el-tooltip>
            <el-button
              v-else
              type="danger"
              size="small"
              text
              :loading="cancelMutation.loading.value && currentActionId.value === scope.row.id"
              :disabled="cancelMutation.locked.value"
              @click="handleCancel(scope.row)"
            >取消</el-button>
          </template>
          <el-button
            v-if="isAdmin && scope.row.status === RepairStatus.FINISHED"   
            type="danger"
            size="small"
            text
            @click="handleDelete(scope.row)"
            :loading="deleteMutation.loading.value && currentActionId.value === scope.row.id"
            :disabled="deleteMutation.locked.value"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="listPage.hasError.value && !listPage.loading.value" class="error-wrapper">
      <el-alert :title="listPage.error.value?.message || '加载失败，请稍后重试'" type="error" show-icon :closable="false">
        <template #default>
          <el-button type="primary" size="small" style="margin-top: 10px" @click="listPage.fetch">重试</el-button>
        </template>
      </el-alert>
    </div>
    <el-empty v-else-if="listPage.isEmpty.value" description="暂无报修记录" />

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="listPage.pagination.currentPage"
        v-model:page-size="listPage.pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="listPage.pagination.total"
        layout="total, sizes, prev, pager, next, jumper"        
        @size-change="listPage.handleSizeChange"
        @current-change="listPage.handleCurrentChange"
        :disabled="listPage.loading.value"
      />
    </div>

    <el-dialog v-model="reportDialogVisible" title="申请报修" width="500px">
      <el-form ref="reportFormRef" :model="reportForm" :rules="reportFormRules" label-width="100px">
        <el-form-item label="设备" prop="equipmentId">
          <el-select v-model="reportForm.equipmentId" placeholder="选择设备" filterable style="width: 100%" @change="onEquipmentChange" v-loading="reportEquipmentsReq.loading.value">
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
        <el-button @click="reportDialogVisible = false" :disabled="reportMutation.loading.value">取消</el-button>
        <el-button type="primary" @click="handleReportSubmit" :loading="reportMutation.loading.value" :disabled="reportMutation.locked.value">提交报修</el-button>
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
        <el-button @click="finishDialogVisible = false" :disabled="finishMutation.loading.value">取消</el-button>
        <el-button type="success" @click="handleFinishSubmit" :loading="finishMutation.loading.value" :disabled="finishMutation.locked.value">确认完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="维修详情" width="600px">
      <div v-loading="detailReq.loading.value">
        <template v-if="detailReq.error.value">
          <el-alert :title="detailReq.error.value?.message || '加载详情失败'" type="error" show-icon :closable="false" />
        </template>
        <el-descriptions :column="2" border v-else-if="currentRepair"> 
          <el-descriptions-item label="维修单号">{{ currentRepair.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getRepairStatusType(currentRepair.status)">{{ getRepairStatusText(currentRepair.status) }}</el-tag>
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
          <template v-if="currentRepair.status === RepairStatus.FINISHED">   
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
        <el-empty v-else-if="!detailReq.loading.value" description="暂无数据" />
      </div>
      <template #footer>
        <el-button v-if="canFinishByRow(currentRepair)" type="success" @click="switchToFinish" :disabled="finishMutation.locked.value">去完成维修</el-button>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'        
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
import { useRequest, useMutation } from '../composables/useRequest'
import { useListPage } from '../composables/useListPage'
import { useOperationCache } from '../composables/useOperationCache'

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
const currentActionId = ref(null)

const repairOpCache = useOperationCache({
  buildCacheKey: (repair, operation) => `${repair.id}_${operation}`,
  checkOperation: (repair, operation) => checkRepairOperation(repair.id, operation)
})

function getRepairOperationAllowed(repair, operation) {
  const cached = repairOpCache.getOrCheck(repair, operation)
  if (cached) return cached.allowed

  if (operation === BusinessOperation.FINISH_REPAIR) {
    return repair.status === RepairStatus.REPORTED || repair.status === RepairStatus.IN_PROGRESS
  }
  if (operation === BusinessOperation.CANCEL_REPAIR) {
    return repair.status === RepairStatus.REPORTED
  }
  return true
}

function getRepairOperationDisabledReason(repair, operation) {
  const cached = repairOpCache.getOrCheck(repair, operation)
  if (cached) return cached.errorMessage || '当前状态不允许此操作'

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
  await repairOpCache.prefetch(repairs, operations)
}

const listPage = useListPage({
  apiPath: '/repairs',
  initialSearchForm: {
    status: ''
  },
  buildParams: (sf) => {
    const params = {}
    if (isTeacher.value) {
      params.userId = userStore.user.id
    }
    if (sf.status) {
      params.status = sf.status
    }
    return params
  },
  onDataLoaded: (content) => {
    if (content && content.length > 0 && canReport.value) {
      prefetchRepairOperations(content)
    }
    repairOpCache.clear()
  }
})

const reportEquipmentsReq = useRequest(
  () => request.get('/equipments', { params: { page: 1, size: 1000 } }),
  {
    showErrorMessage: true,
    errorMessage: '加载设备列表失败',
    onSuccess: (data) => {
      equipments.value = data.content || []
    }
  }
)

const detailReq = useRequest(
  (id) => request.get(`/repairs/${id}`),
  {
    showErrorMessage: false,
    onSuccess: (data) => {
      currentRepair.value = data
    }
  }
)

const reportMutation = useMutation(
  (payload) => request.post('/repairs', payload),
  {
    successMessage: '已报修',
    errorMessage: '报修失败',
    onSuccess: () => {
      reportDialogVisible.value = false
      repairOpCache.clear()
      listPage.fetch()
    }
  }
)

const finishMutation = useMutation(
  ({ id, payload }) => request.put(`/repairs/${id}/finish`, payload),
  {
    successMessage: '维修已完成',
    errorMessage: '操作失败',
    onSuccess: (result, { id }) => {
      if (result) {
        listPage.updateRow(id, result)
      }
      finishDialogVisible.value = false
      repairOpCache.clear()
      listPage.refresh()
    }
  }
)

const cancelMutation = useMutation(
  (id) => request.delete(`/repairs/${id}`),
  {
    successMessage: '已取消',
    errorMessage: '取消失败',
    onSuccess: () => {
      repairOpCache.clear()
      listPage.refresh()
    }
  }
)

const deleteMutation = useMutation(
  (id) => request.delete(`/repairs/${id}`),
  {
    successMessage: '删除成功',
    errorMessage: '删除失败',
    onSuccess: () => {
      repairOpCache.clear()
      listPage.refresh()
    }
  }
)

const onEquipmentChange = () => {
}

const showReportDialog = async () => {
  reportForm.value = {
    equipmentId: null,
    description: ''
  }
  reportDialogVisible.value = true
  try {
    await reportEquipmentsReq.run()
  } catch (e) {}
}

const handleReportSubmit = async () => {
  if (!reportFormRef.value) return
  await reportFormRef.value.validate()

  const selectedEquipment = equipments.value.find(eq => eq.id === reportForm.value.equipmentId)
  if (!canRepairEquipment(selectedEquipment)) {
    ElMessage.error(getEquipmentDisabledReason(selectedEquipment, BusinessOperation.REPORT_REPAIR))
    return
  }

  if (selectedEquipment.hasActiveRepair) {
    ElMessage.error('该设备存在未完成的维修记录，请先完成或取消现有维修单')
    return
  }

  const payload = {
    equipment: { id: reportForm.value.equipmentId },
    description: reportForm.value.description,
    reporter: { id: userStore.user.id }
  }
  
  try {
    await reportMutation.mutate(payload)
  } catch (e) {}
}

const canFinishByRow = (row) => {
  if (!row) return false
  if (!isAdmin.value) return false
  return row.status === RepairStatus.REPORTED || row.status === RepairStatus.IN_PROGRESS
}

const showFinishDialog = async (row) => {
  if (finishMutation.locked.value) return
  
  const checkResult = await checkRepairOperation(row.id, BusinessOperation.FINISH_REPAIR)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法完成维修')
    repairOpCache.set(`${row.id}_${BusinessOperation.FINISH_REPAIR}`, checkResult)
    listPage.refresh()
    return
  }
  
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
  
  if (finishMutation.locked.value) return

  const checkResult = await checkRepairOperation(finishForm.value.id, BusinessOperation.FINISH_REPAIR)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法完成维修')
    repairOpCache.set(`${finishForm.value.id}_${BusinessOperation.FINISH_REPAIR}`, checkResult)
    listPage.refresh()
    finishDialogVisible.value = false
    return
  }

  currentActionId.value = finishForm.value.id
  try {
    const payload = {
      repairConclusion: finishForm.value.repairConclusion,        
      repairCompany: finishForm.value.repairCompany || null,      
      cost: finishForm.value.cost ?? null
    }
    await finishMutation.mutate({ id: finishForm.value.id, payload })
  } finally {
    currentActionId.value = null
  }
}

const showDetailDialog = async (row) => {
  currentRepair.value = null
  detailDialogVisible.value = true
  try {
    await detailReq.run(row.id)
  } catch (e) {}
}

const switchToFinish = () => {
  detailDialogVisible.value = false
  if (currentRepair.value) {
    showFinishDialog(currentRepair.value)
  }
}

const handleCancel = (row) => {
  if (cancelMutation.locked.value) return
  
  checkRepairOperation(row.id, BusinessOperation.CANCEL_REPAIR).then(checkResult => {
    if (!checkResult.allowed) {
      ElMessage.warning(checkResult.errorMessage || '当前状态无法取消')
      repairOpCache.set(`${row.id}_${BusinessOperation.CANCEL_REPAIR}`, checkResult)
      listPage.refresh()
      return
    }

    ElMessageBox.confirm('确认取消该报修？', '提示', {     
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      currentActionId.value = row.id
      try {
        await cancelMutation.mutate(row.id)
      } finally {
        currentActionId.value = null
      }
    }).catch(() => {
      currentActionId.value = null
    })
  })
}

const handleDelete = (row) => {
  if (deleteMutation.locked.value) return

  ElMessageBox.confirm('确认删除该维修记录？删除后不可恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    currentActionId.value = row.id
    try {
      await deleteMutation.mutate(row.id)
    } finally {
      currentActionId.value = null
    }
  }).catch(() => {
    currentActionId.value = null
  })
}

onMounted(() => {})
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

.error-wrapper {
  margin-top: 20px;
}
</style>
