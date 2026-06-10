<template>
  <el-drawer v-model="visible" title="实验室详情" size="680px" :destroy-on-close="true" @close="handleClose">
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

        <div class="alert-row" v-if="detailData.expiringCount > 0 || detailData.activeBorrowCount > 0 || detailData.activeRepairCount > 0">
          <div class="alert-card" v-if="detailData.expiringCount > 0" @click="scrollToSection('expiring')">
            <el-icon><Warning /></el-icon>
            <span>即将到期 <strong>{{ detailData.expiringCount }}</strong> 台</span>
          </div>
          <div class="alert-card alert-card--warning" v-if="detailData.activeBorrowCount > 0" @click="scrollToSection('borrows')">
            <el-icon><User /></el-icon>
            <span>借用中 <strong>{{ detailData.activeBorrowCount }}</strong> 台</span>
          </div>
          <div class="alert-card alert-card--danger" v-if="detailData.activeRepairCount > 0" @click="scrollToSection('repairs')">
            <el-icon><SetUp /></el-icon>
            <span>维修中 <strong>{{ detailData.activeRepairCount }}</strong> 台</span>
          </div>
        </div>

        <el-divider content-position="left" id="section-expiring">即将到期设备（30天内）</el-divider>
        <el-table v-if="detailData.expiringEquipments && detailData.expiringEquipments.length > 0" :data="detailData.expiringEquipments" size="small" style="width: 100%">
          <el-table-column prop="code" label="编号" width="110" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="expiryDate" label="到期日" width="110" />
          <el-table-column label="剩余" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.remainingDays <= 7 ? 'danger' : 'warning'" size="small">{{ scope.row.remainingDays }}天</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无即将到期设备" :image-size="40" />

        <el-divider content-position="left" id="section-borrows">当前借用中</el-divider>
        <el-table v-if="detailData.activeBorrows && detailData.activeBorrows.length > 0" :data="detailData.activeBorrows" size="small" style="width: 100%">
          <el-table-column prop="equipmentCode" label="设备编号" width="110" />
          <el-table-column prop="equipmentName" label="设备名称" />
          <el-table-column prop="applicantName" label="借用人" width="80" />
          <el-table-column label="借用时段" width="180">
            <template #default="scope">
              <div class="time-range">{{ formatTime(scope.row.startTime) }}</div>
              <div class="time-range">{{ formatTime(scope.row.endTime) }}</div>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="detailData.activeBorrowCount > 5" class="more-hint">共 {{ detailData.activeBorrowCount }} 条，以下省略</div>
        <el-empty v-if="!detailData.activeBorrows || detailData.activeBorrows.length === 0" description="暂无借用记录" :image-size="40" />

        <el-divider content-position="left" id="section-repairs">正在维修设备</el-divider>
        <el-table v-if="detailData.activeRepairs && detailData.activeRepairs.length > 0" :data="detailData.activeRepairs" size="small" style="width: 100%">
          <el-table-column prop="equipmentCode" label="设备编号" width="110" />
          <el-table-column prop="equipmentName" label="设备名称" />
          <el-table-column prop="description" label="故障描述" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="scope">
              <el-tag :type="getRepairStatusType(scope.row.status)" size="small">{{ getRepairStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="detailData.activeRepairCount > 5" class="more-hint">共 {{ detailData.activeRepairCount }} 条，以下省略</div>
        <el-empty v-if="!detailData.activeRepairs || detailData.activeRepairs.length === 0" description="暂无维修记录" :image-size="40" />

        <el-divider content-position="left">设备清单</el-divider>
        <div class="equipment-filter">
          <el-select v-model="equipFilter.status" placeholder="状态筛选" style="width: 120px" clearable @change="handleEquipSearch" @clear="handleEquipSearch">
            <el-option label="正常" value="NORMAL" />
            <el-option label="借用中" value="BORROWED" />
            <el-option label="维修中" value="REPAIRING" />
            <el-option label="报废" value="SCRAPPED" />
          </el-select>
          <el-input v-model="equipFilter.keyword" placeholder="搜索设备名称/编号" style="width: 180px; margin-left: 8px" clearable @clear="handleEquipSearch" @keyup.enter="handleEquipSearch" />
          <el-button type="primary" size="small" style="margin-left: 8px" @click="handleEquipSearch">搜索</el-button>
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
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import request from '../api/request'
import { Warning, User, SetUp } from '@element-plus/icons-vue'

const props = defineProps({
  labId: {
    type: Number,
    default: null
  }
})

const visible = defineModel({ type: Boolean, default: false })

const detailLoading = ref(false)
const detailData = ref(null)

const equipFilter = reactive({ status: '', keyword: '' })
const equipPageData = ref({ content: [], totalPages: 0, totalElements: 0 })
const equipPagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

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

const formatTime = (val) => {
  if (!val) return '-'
  return val.replace('T', ' ').substring(0, 16)
}

const scrollToSection = (section) => {
  const el = document.getElementById(`section-${section}`)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const fetchDetail = async () => {
  if (!props.labId) return
  detailLoading.value = true
  detailData.value = null
  try {
    const response = await request.get(`/labs/${props.labId}/detail`)
    detailData.value = response
    fetchEquipments()
  } catch (e) {
    // handled in request.js
  } finally {
    detailLoading.value = false
  }
}

const fetchEquipments = async () => {
  if (!props.labId) return
  const params = {
    page: equipPagination.currentPage,
    size: equipPagination.pageSize,
    labId: props.labId
  }
  if (equipFilter.status) {
    params.status = equipFilter.status
  }
  if (equipFilter.keyword) {
    params.name = equipFilter.keyword
  }
  const response = await request.get('/equipments', { params })
  equipPageData.value = response
  equipPagination.total = response.totalElements
}

const handleEquipSearch = () => {
  equipPagination.currentPage = 1
  fetchEquipments()
}

const handleClose = () => {
  detailData.value = null
  equipFilter.status = ''
  equipFilter.keyword = ''
  equipPagination.currentPage = 1
  equipPagination.pageSize = 10
  equipPageData.value = { content: [], totalPages: 0, totalElements: 0 }
}

watch(visible, (val) => {
  if (val && props.labId) {
    fetchDetail()
  }
})
</script>

<style scoped>
:deep(.el-drawer__body) {
  padding: 20px;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #303133;
}

.stats-row {
  display: flex;
  gap: 12px;
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

.alert-row {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.alert-card {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #e6a23c;
  transition: box-shadow 0.2s;
}

.alert-card:hover {
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.2);
}

.alert-card--warning {
  background: #ecf5ff;
  border-color: #d9ecff;
  color: #409eff;
}

.alert-card--warning:hover {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.alert-card--danger {
  background: #fef0f0;
  border-color: #fde2e2;
  color: #f56c6c;
}

.alert-card--danger:hover {
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.2);
}

.time-range {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.more-hint {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding: 6px 0;
}

.equipment-filter {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
