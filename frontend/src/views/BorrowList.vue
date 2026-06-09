<template>
  <div>
    <div class="header-actions">
      <el-button v-if="isTeacher" type="primary" @click="showApplyDialog">申请借用</el-button>
      <el-select v-model="searchForm.status" placeholder="状态筛选" style="width: 120px; margin-left: 10px" clearable @clear="handleSearch">
        <el-option label="待审批" value="PENDING" />
        <el-option label="已批准" value="APPROVED" />
        <el-option label="已归还" value="RETURNED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="handleSearch">搜索</el-button>
      <el-button style="margin-left: 10px" @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="pageData.content" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="equipment.name" label="设备" />
      <el-table-column prop="applicant.name" label="申请人" />
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="endTime" label="结束时间" width="160" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批信息" min-width="200">
        <template #default="scope">
          <template v-if="scope.row.status === 'APPROVED' || scope.row.status === 'RETURNED'">
            <div class="approval-info">审批人：{{ scope.row.approver?.name || '-' }}</div>
            <div class="approval-info">审批时间：{{ scope.row.approveTime || '-' }}</div>
          </template>
          <template v-else-if="scope.row.status === 'REJECTED'">
            <div class="approval-info">审批人：{{ scope.row.approver?.name || '-' }}</div>
            <div class="approval-info">拒绝时间：{{ scope.row.rejectTime || '-' }}</div>
            <div class="approval-info reject-reason">原因：{{ scope.row.rejectReason || '-' }}</div>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="scope">
          <div v-if="isAdmin && scope.row.status === 'PENDING'">
            <el-button type="success" size="small" @click="handleApprove(scope.row)">批准</el-button>
            <el-button type="danger" size="small" @click="handleReject(scope.row)">拒绝</el-button>
          </div>
          <div v-if="scope.row.status === 'PENDING' && (isAdmin || scope.row.applicant.id === userStore.user.id)">
            <el-button type="warning" size="small" @click="handleCancel(scope.row)">取消</el-button>
          </div>
          <div v-if="scope.row.status === 'APPROVED' && (isAdmin || isTeacher)">
            <el-button type="primary" size="small" @click="handleReturn(scope.row)">归还</el-button>
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

    <el-dialog v-model="dialogVisible" title="申请借用">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备">
          <el-select v-model="form.equipmentId" placeholder="选择设备" filterable>
            <el-option
              v-for="eq in equipments"
              :key="eq.id"
              :label="`${eq.name} (${eq.code})`"
              :value="eq.id"
              :disabled="eq.status !== 'NORMAL'"
            />
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
          />
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="form.purpose" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <BorrowApprovalDialog
      v-model="approvalDialogVisible"
      :action="approvalAction"
      :borrow-record="approvalRecord"
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

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isTeacher = computed(() => userStore.role === 'TEACHER')

const equipments = ref([])
const dialogVisible = ref(false)
const form = ref({})
const dateRange = ref([])

const approvalDialogVisible = ref(false)
const approvalAction = ref('')
const approvalRecord = ref(null)

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

const fetchBorrows = async () => {
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

  const response = await request.get('/borrows', { params })
  pageData.value = response
  pagination.total = response.totalElements
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchBorrows()
}

const resetSearch = () => {
  searchForm.status = ''
  pagination.currentPage = 1
  fetchBorrows()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchBorrows()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchBorrows()
}

const showApplyDialog = async () => {
  const params = {
    page: 1,
    size: 1000
  }
  const response = await request.get('/equipments', { params })
  equipments.value = response.content
  form.value = {}
  dateRange.value = []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.equipmentId || !dateRange.value || dateRange.value.length < 2) {
    ElMessage.error('请填写完整信息')
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
    await request.post('/borrows', payload)
    dialogVisible.value = false
    fetchBorrows()
    ElMessage.success('申请已提交')
  } catch (e) {
    // handled in request.js
  }
}

const handleApprove = (row) => {
  approvalAction.value = 'approve'
  approvalRecord.value = row
  approvalDialogVisible.value = true
}

const handleReject = (row) => {
  approvalAction.value = 'reject'
  approvalRecord.value = row
  approvalDialogVisible.value = true
}

const handleApprovalConfirm = async ({ action, rejectReason }) => {
  const id = approvalRecord.value.id
  try {
    if (action === 'approve') {
      await request.put(`/borrows/${id}/approve?approverId=${userStore.user.id}`)
      ElMessage.success('已批准')
    } else {
      await request.put(`/borrows/${id}/reject?approverId=${userStore.user.id}&rejectReason=${encodeURIComponent(rejectReason)}`)
      ElMessage.success('已拒绝')
    }
    fetchBorrows()
  } catch (e) {
    // handled in request.js
  }
}

const handleReturn = async (row) => {
  await request.put(`/borrows/${row.id}/return`)
  fetchBorrows()
  ElMessage.success('已归还')
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确认取消申请？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/borrows/${row.id}`)
    fetchBorrows()
    ElMessage.success('已取消')
  })
}

const getStatusType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'RETURNED') return 'info'
  return 'danger'
}

const getStatusText = (status) => {
  if (status === 'APPROVED') return '已批准'
  if (status === 'PENDING') return '待审批'
  if (status === 'RETURNED') return '已归还'
  if (status === 'REJECTED') return '已拒绝'
  return status
}

onMounted(fetchBorrows)
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

.approval-info {
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
}

.reject-reason {
  color: #f56c6c;
}
</style>
