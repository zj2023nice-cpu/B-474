<template>
  <div>
    <div class="header-actions">
      <el-button v-if="canApply" type="primary" @click="showApplyDialog" :loading="applyEquipmentsReq.loading.value">申请借用</el-button>
      <el-alert v-if="isStudent" type="info" :closable="false" show-icon class="student-tip">
        <template #title>学生账号仅可查看借用记录，如需申请请联系教师</template>
      </el-alert>
      <el-select v-model="listPage.searchForm.status" placeholder="状态筛选" style="width: 130px; margin-left: 10px" clearable @change="listPage.handleSearch">
        <el-option label="待审批" :value="BorrowStatus.PENDING" />
        <el-option label="已批准" :value="BorrowStatus.APPROVED" />
        <el-option label="已归还" :value="BorrowStatus.RETURNED" />
        <el-option label="已拒绝" :value="BorrowStatus.REJECTED" />
        <el-option label="已取消" :value="BorrowStatus.CANCELLED" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="listPage.handleSearch" :loading="listPage.loading.value">搜索</el-button>
      <el-button style="margin-left: 10px" @click="listPage.resetSearch" :disabled="listPage.loading.value">重置</el-button>
    </div>

    <el-table :data="listPage.pageData.content" style="width: 100%" v-loading="listPage.loading.value">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="equipment.name" label="设备" min-width="100">
        <template #default="scope">{{ scope.row.equipment?.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="applicant.name" label="申请人" width="90">
        <template #default="scope">{{ scope.row.applicant?.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="endTime" label="结束时间" width="160" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="getBorrowStatusType(scope.row.status)">
            {{ getBorrowStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批人" width="90">
        <template #default="scope">
          <span>{{ scope.row.approver?.name || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审批时间" width="160">
        <template #default="scope">
          <span>{{ scope.row.approveTime || scope.row.rejectTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="拒绝原因" min-width="150">
        <template #default="scope">
          <span :class="{ 'reject-reason': scope.row.rejectReason }">
            {{ scope.row.rejectReason || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column v-if="canApply" label="操作" width="280" fixed="right">
        <template #default="scope">
          <div v-if="isAdmin && scope.row.status === BorrowStatus.PENDING">
            <el-tooltip 
              v-if="!getBorrowOperationAllowed(scope.row, BusinessOperation.APPROVE_BORROW)"
              :content="getBorrowOperationDisabledReason(scope.row, BusinessOperation.APPROVE_BORROW)"
              placement="top"
            >
              <el-button 
                type="success" 
                size="small" 
                disabled
              >批准</el-button>
            </el-tooltip>
            <el-tooltip 
              v-if="!getBorrowOperationAllowed(scope.row, BusinessOperation.APPROVE_BORROW)"
              :content="getBorrowOperationDisabledReason(scope.row, BusinessOperation.APPROVE_BORROW)"
              placement="top"
            >
              <el-button 
                type="danger" 
                size="small" 
                disabled
              >拒绝</el-button>
            </el-tooltip>
            <template v-else>
              <el-button 
                type="success" 
                size="small" 
                :loading="approvalMutation.loading.value" 
                :disabled="approvalMutation.locked.value"
                @click="handleApprove(scope.row)"
              >批准</el-button>
              <el-button 
                type="danger" 
                size="small" 
                :loading="approvalMutation.loading.value" 
                :disabled="approvalMutation.locked.value"
                @click="handleReject(scope.row)"
              >拒绝</el-button>
            </template>
          </div>
          <div v-if="scope.row.status === BorrowStatus.PENDING && (isAdmin || scope.row.applicant?.id === userStore.user.id)">
            <el-tooltip 
              v-if="!getBorrowOperationAllowed(scope.row, BusinessOperation.CANCEL_BORROW)"
              :content="getBorrowOperationDisabledReason(scope.row, BusinessOperation.CANCEL_BORROW)"
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
              :loading="cancelMutation.loading.value && currentActionId.value === scope.row.id"
              :disabled="cancelMutation.locked.value"
              @click="handleCancel(scope.row)"
            >取消</el-button>
          </div>
          <div v-if="scope.row.status === BorrowStatus.APPROVED">
            <el-tooltip 
              v-if="!getBorrowOperationAllowed(scope.row, BusinessOperation.RETURN_EQUIPMENT)"
              :content="getBorrowOperationDisabledReason(scope.row, BusinessOperation.RETURN_EQUIPMENT)"
              placement="top"
            >
              <el-button 
                type="primary" 
                size="small" 
                disabled
              >归还</el-button>
            </el-tooltip>
            <el-button 
              v-else
              type="primary" 
              size="small" 
              :loading="returnMutation.loading.value && currentActionId.value === scope.row.id"
              :disabled="returnMutation.locked.value"
              @click="handleReturn(scope.row)"
            >归还</el-button>
          </div>
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
    <el-empty v-else-if="listPage.isEmpty.value" description="暂无借用记录" />

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

    <el-dialog v-model="dialogVisible" title="申请借用" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备">
          <el-select
            v-model="form.equipmentId"
            placeholder="选择设备"
            filterable
            @change="triggerConflictCheck"
            v-loading="applyEquipmentsReq.loading.value"
          >
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="`${eq.name} (${eq.code})`"
              :value="eq.id"
              :disabled="!canBorrowEquipment(eq)"
            >
              <span>{{ eq.name }} ({{ eq.code }})</span>
              <el-tag 
                v-if="!canBorrowEquipment(eq)" 
                size="small" 
                type="info"
                style="margin-left: 8px"
              >
                {{ getEquipmentDisabledReason(eq, BusinessOperation.APPLY_BORROW) }}
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="借用时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="triggerConflictCheck"
          />
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="form.purpose" type="textarea" />
        </el-form-item>
      </el-form>

      <div v-if="conflictCheckLoading" class="conflict-check-section">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>正在检查时间冲突...</template>
        </el-alert>
      </div>
      <div v-else-if="conflictCheckResult && conflictCheckResult.hasConflict" class="conflict-check-section">
        <el-alert type="warning" :closable="false" show-icon>
          <template #title>
            发现 {{ conflictCheckResult.conflicts.length }} 个时间冲突
          </template>
          <div class="conflict-list">
            <div
              v-for="conflict in conflictCheckResult.conflicts"
              :key="conflict.borrowId"
              class="conflict-item"
            >
              <el-tag size="small" :type="conflict.status === BorrowStatus.APPROVED ? 'danger' : 'warning'">
                {{ conflict.status === BorrowStatus.APPROVED ? '已批准' : '待审批' }}
              </el-tag>
              <span class="conflict-applicant">{{ conflict.applicantName }}</span>
              <span class="conflict-time">
                {{ formatDateTime(conflict.startTime) }} ~ {{ formatDateTime(conflict.endTime) }}
              </span>
            </div>
          </div>
        </el-alert>
      </div>
      <div v-else-if="conflictCheckResult && !conflictCheckResult.hasConflict && canCheckConflict" class="conflict-check-section">
        <el-alert type="success" :closable="false" show-icon>
          <template #title>该时间段暂无冲突，可以申请</template>
        </el-alert>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false" :disabled="submitMutation.loading.value">取消</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :disabled="conflictCheckLoading || (conflictCheckResult && conflictCheckResult.hasConflict) || !form.equipmentId || submitMutation.locked.value"
          :loading="submitMutation.loading.value"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

    <BorrowApprovalDialog
      ref="approvalDialogRef"
      v-model="approvalDialogVisible"
      :action="approvalAction"
      :borrow-record="approvalRecord"
      :submitting="approvalMutation.loading.value"
      @confirm="handleApprovalConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import BorrowApprovalDialog from '../components/BorrowApprovalDialog.vue'
import { 
  BorrowStatus,
  BusinessOperation,
  getBorrowStatusText,
  getBorrowStatusType,
  canBorrowEquipment,
  getEquipmentDisabledReason
} from '../constants/statusConstants'
import { checkBorrowOperation } from '../api/stateConstraint'
import { useRequest, useMutation } from '../composables/useRequest'
import { useListPage } from '../composables/useListPage'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isTeacher = computed(() => userStore.role === 'TEACHER')
const canApply = computed(() => isAdmin.value || isTeacher.value)
const isStudent = computed(() => userStore.role === 'STUDENT')

const canCheckConflict = computed(() => {
  return form.value.equipmentId &&
         dateRange.value &&
         dateRange.value.length === 2 &&
         dateRange.value[0] &&
         dateRange.value[1]
})

const equipments = ref([])
const dialogVisible = ref(false)
const form = ref({})
const dateRange = ref([])

const conflictCheckLoading = ref(false)
const conflictCheckResult = ref(null)
let conflictCheckTimer = null

const approvalDialogVisible = ref(false)
const approvalAction = ref('')
const approvalRecord = ref(null)
const approvalDialogRef = ref(null)
const currentActionId = ref(null)

const borrowOperationCache = ref(new Map())

const listPage = useListPage({
  apiPath: '/borrows',
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
    if (content && content.length > 0 && canApply.value) {
      prefetchBorrowOperations(content)
    }
    borrowOperationCache.value.clear()
  }
})

const applyEquipmentsReq = useRequest(
  () => request.get('/equipments', { params: { page: 1, size: 1000 } }),
  {
    showErrorMessage: true,
    errorMessage: '加载设备列表失败',
    onSuccess: (data) => {
      equipments.value = data.content || []
    }
  }
)

const conflictReq = useRequest(
  (params) => request.get('/borrows/check-conflicts', { params }),
  {
    onSuccess: (result) => {
      conflictCheckResult.value = result
    }
  }
)

const submitMutation = useMutation(
  (payload) => request.post('/borrows', payload),
  {
    successMessage: '申请已提交',
    errorMessage: '提交失败',
    onSuccess: () => {
      dialogVisible.value = false
      borrowOperationCache.value.clear()
      listPage.fetch()
    }
  }
)

const approvalMutation = useMutation(
  ({ action, id, rejectReason }) => {
    if (action === 'approve') {
      return request.put(`/borrows/${id}/approve`)
    }
    return request.put(`/borrows/${id}/reject`, { rejectReason })
  },
  {
    errorMessage: '操作失败',
    onSuccess: (result, { action }) => {
      ElMessage.success(action === 'approve' ? '已批准' : '已拒绝')
      approvalDialogRef.value?.handleSuccess()
      if (approvalRecord.value?.id != null && result) {
        const idx = listPage.pageData.value.content.findIndex(item => item.id === approvalRecord.value.id)
        if (idx !== -1) {
          listPage.pageData.value.content.splice(idx, 1, result)
        }
      }
      borrowOperationCache.value.clear()
      listPage.refresh()
    }
  }
)

const returnMutation = useMutation(
  (id) => request.put(`/borrows/${id}/return`),
  {
    successMessage: '已归还',
    errorMessage: '归还失败',
    onSuccess: (result, id) => {
      const idx = listPage.pageData.value.content.findIndex(item => item.id === id)
      if (idx !== -1 && result) {
        listPage.pageData.value.content.splice(idx, 1, result)
      }
      borrowOperationCache.value.clear()
      listPage.refresh()
    }
  }
)

const cancelMutation = useMutation(
  (id) => request.put(`/borrows/${id}/cancel`),
  {
    successMessage: '已取消',
    errorMessage: '取消失败',
    onSuccess: (result, id) => {
      const idx = listPage.pageData.value.content.findIndex(item => item.id === id)
      if (idx !== -1 && result) {
        listPage.pageData.value.content.splice(idx, 1, result)
      }
      borrowOperationCache.value.clear()
      listPage.refresh()
    }
  }
)

function getBorrowOperationAllowed(borrow, operation) {
  const cacheKey = `${borrow.id}_${operation}`
  if (borrowOperationCache.value.has(cacheKey)) {
    return borrowOperationCache.value.get(cacheKey).allowed
  }
  if (operation === BusinessOperation.APPROVE_BORROW) {
    return borrow.status === BorrowStatus.PENDING
  }
  if (operation === BusinessOperation.CANCEL_BORROW) {
    return borrow.status === BorrowStatus.PENDING
  }
  if (operation === BusinessOperation.RETURN_EQUIPMENT) {
    return borrow.status === BorrowStatus.APPROVED
  }
  return true
}

function getBorrowOperationDisabledReason(borrow, operation) {
  const cacheKey = `${borrow.id}_${operation}`
  if (borrowOperationCache.value.has(cacheKey)) {
    const result = borrowOperationCache.value.get(cacheKey)
    return result.errorMessage || '当前状态不允许此操作'
  }
  if (operation === BusinessOperation.APPROVE_BORROW && borrow.status !== BorrowStatus.PENDING) {
    return '仅待审批状态的申请可以审批'
  }
  if (operation === BusinessOperation.CANCEL_BORROW && borrow.status !== BorrowStatus.PENDING) {
    return '仅待审批状态的申请可以取消'
  }
  if (operation === BusinessOperation.RETURN_EQUIPMENT && borrow.status !== BorrowStatus.APPROVED) {
    return '仅已批准状态的借用可以归还'
  }
  return '当前状态不允许此操作'
}

async function prefetchBorrowOperations(borrows) {
  const operations = [
    BusinessOperation.APPROVE_BORROW,
    BusinessOperation.CANCEL_BORROW,
    BusinessOperation.RETURN_EQUIPMENT
  ]
  
  for (const borrow of borrows) {
    for (const op of operations) {
      const cacheKey = `${borrow.id}_${op}`
      if (!borrowOperationCache.value.has(cacheKey)) {
        try {
          const result = await checkBorrowOperation(borrow.id, op)
          borrowOperationCache.value.set(cacheKey, result)
        } catch (e) {
          console.warn(`Failed to check borrow operation ${op} for borrow ${borrow.id}:`, e)
        }
      }
    }
  }
}

const showApplyDialog = async () => {
  form.value = {}
  dateRange.value = []
  conflictCheckResult.value = null
  conflictCheckLoading.value = false
  if (conflictCheckTimer) {
    clearTimeout(conflictCheckTimer)
    conflictCheckTimer = null
  }
  dialogVisible.value = true
  try {
    await applyEquipmentsReq.run()
  } catch (e) {}
}

const triggerConflictCheck = () => {
  if (conflictCheckTimer) {
    clearTimeout(conflictCheckTimer)
  }
  conflictCheckTimer = setTimeout(() => {
    checkConflicts()
  }, 500)
}

const checkConflicts = async () => {
  if (!canCheckConflict.value) {
    conflictCheckResult.value = null
    return
  }

  const params = {
    equipmentId: form.value.equipmentId,
    startTime: dateRange.value[0],
    endTime: dateRange.value[1]
  }

  conflictCheckLoading.value = true
  conflictCheckResult.value = null

  try {
    await conflictReq.run(params)
  } catch (e) {
    console.error('Conflict check failed:', e)
  } finally {
    conflictCheckLoading.value = false
  }
}

const formatDateTime = (datetime) => {
  if (!datetime) return ''
  if (typeof datetime === 'string') return datetime
  return datetime
}

const handleSubmit = async () => {
  if (!form.value.equipmentId || !dateRange.value || dateRange.value.length < 2) {
    ElMessage.error('请填写完整信息')
    return
  }

  const selectedEquipment = equipments.value.find(eq => eq.id === form.value.equipmentId)
  if (!canBorrowEquipment(selectedEquipment)) {
    ElMessage.error(getEquipmentDisabledReason(selectedEquipment, BusinessOperation.APPLY_BORROW))
    return
  }

  const payload = {
    equipment: { id: form.value.equipmentId },
    applicant: { id: userStore.user.id },
    startTime: dateRange.value[0],
    endTime: dateRange.value[1],
    purpose: form.value.purpose,
    applyDate: new Date().toISOString().replace('T', ' ').split('.')[0]
  }

  try {
    await submitMutation.mutate(payload)
  } catch (e) {
    if (e.message && e.message.includes('冲突记录')) {
      ElMessageBox.alert(
        e.message.replace(/\n/g, '<br/>'),
        '申请失败',
        {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '我知道了',
          type: 'warning'
        }
      )
    }
  }
}

const handleApprove = async (row) => {
  if (approvalMutation.locked.value) return
  
  const checkResult = await checkBorrowOperation(row.id, BusinessOperation.APPROVE_BORROW)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法审批')
    borrowOperationCache.value.set(`${row.id}_${BusinessOperation.APPROVE_BORROW}`, checkResult)
    listPage.refresh()
    return
  }
  
  approvalAction.value = 'approve'
  approvalRecord.value = { ...row }
  approvalDialogVisible.value = true
}

const handleReject = async (row) => {
  if (approvalMutation.locked.value) return
  
  const checkResult = await checkBorrowOperation(row.id, BusinessOperation.APPROVE_BORROW)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法审批')
    borrowOperationCache.value.set(`${row.id}_${BusinessOperation.APPROVE_BORROW}`, checkResult)
    listPage.refresh()
    return
  }
  
  approvalAction.value = 'reject'
  approvalRecord.value = { ...row }
  approvalDialogVisible.value = true
}

const handleApprovalConfirm = async ({ action, rejectReason }) => {
  if (approvalMutation.locked.value) return

  const id = approvalRecord.value?.id
  if (!id) return

  try {
    await approvalMutation.mutate({ action, id, rejectReason })
  } catch (e) {
    approvalDialogRef.value?.handleError(e.message || '操作失败，请重试')
    borrowOperationCache.value.clear()
    listPage.refresh()
  }
}

const handleReturn = async (row) => {
  if (returnMutation.locked.value) return
  
  const checkResult = await checkBorrowOperation(row.id, BusinessOperation.RETURN_EQUIPMENT)
  if (!checkResult.allowed) {
    ElMessage.warning(checkResult.errorMessage || '当前状态无法归还')
    borrowOperationCache.value.set(`${row.id}_${BusinessOperation.RETURN_EQUIPMENT}`, checkResult)
    listPage.refresh()
    return
  }

  currentActionId.value = row.id
  try {
    await returnMutation.mutate(row.id)
  } catch (e) {
    borrowOperationCache.value.clear()
    listPage.refresh()
  } finally {
    currentActionId.value = null
  }
}

const handleCancel = (row) => {
  if (cancelMutation.locked.value) return
  
  checkBorrowOperation(row.id, BusinessOperation.CANCEL_BORROW).then(checkResult => {
    if (!checkResult.allowed) {
      ElMessage.warning(checkResult.errorMessage || '当前状态无法取消')
      borrowOperationCache.value.set(`${row.id}_${BusinessOperation.CANCEL_BORROW}`, checkResult)
      listPage.refresh()
      return
    }

    ElMessageBox.confirm('确认取消申请？取消后无法恢复。', '提示', {
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

.reject-reason {
  color: #f56c6c;
}

.conflict-check-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.conflict-list {
  margin-top: 12px;
}

.conflict-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 13px;
}

.conflict-item:last-child {
  margin-bottom: 0;
}

.conflict-applicant {
  font-weight: 500;
  min-width: 60px;
}

.conflict-time {
  color: #606266;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.error-wrapper {
  margin-top: 20px;
}
</style>
