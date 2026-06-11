<template>
  <div>
    <div class="header-actions">
      <el-button v-if="canCreateEquipment" type="primary" @click="showAddDialog" :loading="addMutation.loading.value" :disabled="addMutation.locked.value">新增设备</el-button>
      <el-input v-model="listPage.searchForm.name" placeholder="搜索名称" style="width: 150px; margin-left: 10px" clearable @clear="listPage.handleSearch" />
      <el-input v-model="listPage.searchForm.code" placeholder="搜索编号" style="width: 150px; margin-left: 10px" clearable @clear="listPage.handleSearch" />
      <el-select v-model="listPage.searchForm.status" placeholder="状态筛选" style="width: 120px; margin-left: 10px" clearable @clear="listPage.handleSearch">
        <el-option label="正常" value="NORMAL" />
        <el-option label="借用中" value="BORROWED" />
        <el-option label="维修中" value="REPAIRING" />
        <el-option label="报废" value="SCRAPPED" />
      </el-select>
      <el-select v-model="listPage.searchForm.labId" placeholder="实验室筛选" style="width: 150px; margin-left: 10px" clearable @clear="listPage.handleSearch">
        <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="lab.id" />
      </el-select>
      <el-button type="primary" style="margin-left: 10px" @click="listPage.handleSearch" :loading="listPage.loading.value">搜索</el-button>
      <el-button style="margin-left: 10px" @click="listPage.resetSearch" :disabled="listPage.loading.value">重置</el-button>
    </div>

    <el-table
      :data="listPage.pageData.content"
      style="width: 100%"
      @row-click="handleRowClick"
      highlight-current-row
      v-loading="listPage.loading.value"
    >
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
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" size="small" @click.stop="openDetail(scope.row)">详情</el-button>
          <el-button
            v-if="canDeleteEquipment"
            type="danger"
            size="small"
            @click.stop="handleDelete(scope.row)"
            :loading="deleteMutation.loading.value && deleteRowId.value === scope.row.id"
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
    <el-empty v-else-if="listPage.isEmpty.value" description="暂无设备数据" />

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
        <el-button @click="dialogVisible = false" :disabled="addMutation.loading.value">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="addMutation.loading.value" :disabled="addMutation.locked.value">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" title="设备详情" size="480px" :destroy-on-close="true">
      <div v-loading="detailReq.loading.value">
        <template v-if="detailReq.error.value">
          <el-alert :title="detailReq.error.value?.message || '加载详情失败'" type="error" show-icon :closable="false">
            <template #default>
              <el-button type="primary" size="small" style="margin-top: 10px" @click="reloadDetail">重试</el-button>
            </template>
          </el-alert>
        </template>
        <template v-else-if="detailData">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="编号">{{ detailData.code }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ detailData.name }}</el-descriptions-item>
            <el-descriptions-item label="型号">{{ detailData.model || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生产厂商">{{ detailData.manufacturer || '-' }}</el-descriptions-item>
            <el-descriptions-item label="采购日期">{{ detailData.purchaseDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="价格">{{ detailData.price != null ? `¥${detailData.price}` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="使用年限">{{ detailData.lifeSpan != null ? `${detailData.lifeSpan} 年` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">所属实验室</el-divider>
          <el-descriptions v-if="detailData.lab" :column="1" border>
            <el-descriptions-item label="实验室名称">{{ detailData.lab.name }}</el-descriptions-item>
            <el-descriptions-item label="建筑">{{ detailData.lab.building || '-' }}</el-descriptions-item>
            <el-descriptions-item label="房间号">{{ detailData.lab.room || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ detailData.lab.picName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detailData.lab.picPhone || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无实验室信息" :image-size="60" />

          <el-divider content-position="left">到期信息</el-divider>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="预计到期日期">{{ detailData.expiryDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="剩余天数">
              <template v-if="detailData.remainingDays != null">
                <el-tag :type="detailData.remainingDays <= 0 ? 'danger' : detailData.remainingDays <= 30 ? 'warning' : 'success'">
                  {{ detailData.remainingDays <= 0 ? '已过期' : `${detailData.remainingDays} 天` }}
                </el-tag>
              </template>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">最近借用记录</el-divider>
          <el-descriptions v-if="detailData.latestBorrow" :column="1" border>
            <el-descriptions-item label="申请人">{{ detailData.latestBorrow.applicantName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ detailData.latestBorrow.applyDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ detailData.latestBorrow.startTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ detailData.latestBorrow.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用途">{{ detailData.latestBorrow.purpose || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getBorrowStatusType(detailData.latestBorrow.status)">{{ getBorrowStatusText(detailData.latestBorrow.status) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无借用记录" :image-size="60" />

          <el-divider content-position="left">最近维修记录</el-divider>
          <el-descriptions v-if="detailData.latestRepair" :column="1" border>
            <el-descriptions-item label="状态">
              <el-tag :type="getRepairStatusType(detailData.latestRepair.status)">{{ getRepairStatusText(detailData.latestRepair.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="故障描述">{{ detailData.latestRepair.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报修时间">{{ detailData.latestRepair.reportDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报修人">{{ detailData.latestRepair.reporterName || '-' }}</el-descriptions-item>
            <template v-if="detailData.latestRepair.status === 'FINISHED'">
              <el-descriptions-item label="维修结论">{{ detailData.latestRepair.repairConclusion || '-' }}</el-descriptions-item>
              <el-descriptions-item label="维修单位">{{ detailData.latestRepair.repairCompany || '-' }}</el-descriptions-item>
              <el-descriptions-item label="维修费用">
                <span v-if="detailData.latestRepair.cost !== null && detailData.latestRepair.cost !== undefined">
                  ¥{{ Number(detailData.latestRepair.cost).toFixed(2) }}
                </span>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ detailData.latestRepair.finishDate || '-' }}</el-descriptions-item>
            </template>
          </el-descriptions>
          <el-empty v-else description="暂无维修记录" :image-size="60" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useRequest, useMutation } from '../composables/useRequest'
import { useListPage } from '../composables/useListPage'
import { PERMISSIONS } from '../constants/roleConstants'

const userStore = useUserStore()
const canCreateEquipment = computed(() => userStore.hasPermission(PERMISSIONS.EQUIPMENT_CREATE))
const canDeleteEquipment = computed(() => userStore.hasPermission(PERMISSIONS.EQUIPMENT_DELETE))

const labs = ref([])
const dialogVisible = ref(false)
const form = ref({})
const drawerVisible = ref(false)
const deleteRowId = ref(null)
const lastDetailId = ref(null)

const listPage = useListPage({
  apiPath: '/equipments',
  initialSearchForm: {
    name: '',
    code: '',
    status: '',
    labId: null
  }
})

const labsReq = useRequest(
  () => request.get('/labs', { params: { page: 1, size: 1000 } }),
  {
    showErrorMessage: true,
    errorMessage: '实验室数据加载失败',
    onSuccess: (data) => {
      labs.value = data.content || []
    }
  }
)

const detailReq = useRequest(
  (id) => request.get(`/equipments/${id}/detail`),
  {
    showErrorMessage: false,
    onSuccess: (data) => {
      detailData.value = data
    }
  }
)

const detailData = ref(null)

const addMutation = useMutation(
  (payload) => request.post('/equipments', payload),
  {
    successMessage: '设备已添加',
    errorMessage: '添加失败',
    onSuccess: () => {
      dialogVisible.value = false
      listPage.fetch()
    }
  }
)

const deleteMutation = useMutation(
  (id) => request.delete(`/equipments/${id}`),
  {
    successMessage: '已删除',
    errorMessage: '删除失败',
    onSuccess: () => {
      listPage.refresh()
    }
  }
)

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

const showAddDialog = () => {
  form.value = { price: 0, lifeSpan: 5 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const payload = {
    ...form.value,
    lab: { id: form.value.labId }
  }
  try {
    await addMutation.mutate(payload)
  } catch (e) {}
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '警告', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .then(async () => {
      deleteRowId.value = row.id
      try {
        await deleteMutation.mutate(row.id)
      } catch (e) {}
      finally {
        deleteRowId.value = null
      }
    })
    .catch(() => {})
}

const openDetail = async (row) => {
  drawerVisible.value = true
  detailData.value = null
  lastDetailId.value = row.id
  try {
    await detailReq.run(row.id)
  } catch (e) {}
}

const reloadDetail = async () => {
  if (lastDetailId.value) {
    try {
      await detailReq.run(lastDetailId.value)
    } catch (e) {}
  }
}

const handleRowClick = (row) => {
  openDetail(row)
}

const getBorrowStatusType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'RETURNED') return 'info'
  if (status === 'REJECTED') return 'danger'
  if (status === 'CANCELLED') return 'info'
  return 'info'
}

const getBorrowStatusText = (status) => {
  if (status === 'APPROVED') return '已批准'
  if (status === 'PENDING') return '待审批'
  if (status === 'RETURNED') return '已归还'
  if (status === 'REJECTED') return '已拒绝'
  if (status === 'CANCELLED') return '已取消'
  return status
}

const getRepairStatusType = (status) => {
  if (status === 'FINISHED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  return 'danger'
}

const getRepairStatusText = (status) => {
  if (status === 'FINISHED') return '已完成'
  if (status === 'IN_PROGRESS') return '维修中'
  if (status === 'REPORTED') return '已报修'
  return status
}

onMounted(() => {
  labsReq.run()
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

.error-wrapper {
  margin-top: 20px;
}

:deep(.el-drawer__body) {
  padding: 20px;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #303133;
}
</style>
