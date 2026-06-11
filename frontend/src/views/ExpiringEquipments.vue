<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备到期管理</span>
          <el-tag v-if="pagination.total > 0" type="warning">共 {{ pagination.total }} 条记录</el-tag>
        </div>
      </template>

      <div class="filter-bar">
        <el-select v-model="searchForm.labId" placeholder="实验室筛选" style="width: 160px" clearable @clear="handleSearch" @change="handleSearch">
          <el-option v-for="lab in labs" :key="lab.id" :label="lab.name" :value="lab.id" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="设备状态" style="width: 120px; margin-left: 10px" clearable @clear="handleSearch" @change="handleSearch">
          <el-option label="正常" value="NORMAL" />
          <el-option label="借用中" value="BORROWED" />
          <el-option label="维修中" value="REPAIRING" />
        </el-select>
        <el-switch v-model="searchForm.expiredOnly" active-text="只看已超期" inactive-text="" style="margin-left: 10px" @change="onExpiredOnlyChange" />
        <el-tooltip v-if="searchForm.expiredOnly" content="只看已超期模式下仅展示能确认超期的设备" placement="top">
          <el-switch v-model="searchForm.includeIncomplete" active-text="含信息不全" inactive-text="" style="margin-left: 10px" disabled @change="handleSearch" />
        </el-tooltip>
        <el-switch v-else v-model="searchForm.includeIncomplete" active-text="含信息不全" inactive-text="" style="margin-left: 10px" @change="handleSearch" />
        <el-select v-model="searchForm.sortBy" placeholder="排序方式" style="width: 140px; margin-left: 10px" @change="handleSearch">
          <el-option label="剩余天数" value="remainingDays" />
          <el-option label="到期日期" value="expiryDate" />
          <el-option label="超期程度" value="overdueDegree" />
        </el-select>
        <el-select v-model="searchForm.sortOrder" style="width: 100px; margin-left: 10px" @change="handleSearch">
          <el-option label="升序" value="asc" />
          <el-option label="降序" value="desc" />
        </el-select>
        <el-button style="margin-left: 10px" @click="resetSearch">重置</el-button>
      </div>

      <el-alert
        v-if="incompleteCount > 0 && searchForm.includeIncomplete"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          有 {{ incompleteCount }} 台设备缺少采购日期或使用年限，无法计算到期信息，请及时补充
        </template>
      </el-alert>

      <el-table :data="pageData.content" style="width: 100%" v-loading="loading" :row-class-name="rowClassName">
        <el-table-column prop="code" label="设备编号" width="120" />
        <el-table-column prop="name" label="设备名称" min-width="120" />
        <el-table-column prop="model" label="型号" width="120" />
        <el-table-column prop="lab.name" label="所属实验室" width="140" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="采购日期" width="110" />
        <el-table-column label="使用年限" width="90">
          <template #default="scope">
            {{ scope.row.lifeSpan != null ? scope.row.lifeSpan + '年' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="到期日期" width="110">
          <template #default="scope">
            <template v-if="scope.row.dataComplete">
              {{ scope.row.expiryDate }}
            </template>
            <template v-else>
              <el-tooltip :content="scope.row.dataIncompleteReason" placement="top">
                <el-tag type="info" size="small">信息不全</el-tag>
              </el-tooltip>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="120">
          <template #default="scope">
            <template v-if="scope.row.dataComplete">
              <el-tag :type="getRemainingDaysType(scope.row.remainingDays)" size="small">
                {{ getRemainingDaysText(scope.row.remainingDays) }}
              </el-tag>
            </template>
            <template v-else>
              <el-tooltip :content="scope.row.dataIncompleteReason" placement="top">
                <span class="incomplete-text">无法计算</span>
              </el-tooltip>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && pageData.content.length === 0 && !hasActiveFilters"
        description="暂无即将到期或已超期的设备"
      />
      <el-empty
        v-if="!loading && pageData.content.length === 0 && hasActiveFilters"
        description="没有符合筛选条件的设备，请调整筛选条件"
      />

      <div class="pagination-container" v-if="pagination.total > 0">
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '../api/request'

function parseDateParts(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') return null
  const trimmed = dateStr.trim()
  const match = trimmed.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
  if (!match) return null
  const year = parseInt(match[1], 10)
  const month = parseInt(match[2], 10)
  const day = parseInt(match[3], 10)
  if (isNaN(year) || isNaN(month) || isNaN(day)) return null
  if (month < 1 || month > 12 || day < 1) return null
  const maxDay = daysInMonth(year, month)
  if (day > maxDay) return null
  return { year, month, day }
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

function formatDateParts(parts) {
  if (!parts) return ''
  return `${parts.year}-${pad2(parts.month)}-${pad2(parts.day)}`
}

function isLeapYear(year) {
  return (year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0)
}

function daysInMonth(year, month) {
  return new Date(year, month, 0).getDate()
}

function addYears(parts, years) {
  if (years == null || years <= 0) return null
  const newYear = parts.year + years
  let newDay = parts.day
  if (parts.month === 2 && parts.day === 29 && !isLeapYear(newYear)) {
    newDay = 28
  }
  return { year: newYear, month: parts.month, day: newDay }
}

function toEpochDay(year, month, day) {
  const a = Math.floor((14 - month) / 12)
  const y = year + 4800 - a
  const m = month + 12 * a - 3
  return day + Math.floor((153 * m + 2) / 5) + 365 * y
    + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) - 32045
}

function daysBetweenDates(fromParts, toParts) {
  return toEpochDay(toParts.year, toParts.month, toParts.day)
    - toEpochDay(fromParts.year, fromParts.month, fromParts.day)
}

function getTodayLocal() {
  const now = new Date()
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() }
}

function calcExpiryDateLocal(purchaseDateStr, lifeSpan) {
  const parts = parseDateParts(purchaseDateStr)
  if (!parts || lifeSpan == null || lifeSpan <= 0) return null
  return addYears(parts, lifeSpan)
}

function calcRemainingDaysLocal(expiryDateStr) {
  const expiryParts = parseDateParts(expiryDateStr)
  if (!expiryParts) return null
  const todayParts = getTodayLocal()
  return daysBetweenDates(todayParts, expiryParts)
}

function enrichExpiryFields(item) {
  const purchaseDate = item.purchaseDate
  const lifeSpan = item.lifeSpan

  if (!purchaseDate || lifeSpan == null || lifeSpan <= 0) {
    item.dataComplete = false
    const reasons = []
    if (!purchaseDate) reasons.push('缺少采购日期')
    if (lifeSpan == null || lifeSpan <= 0) reasons.push('缺少使用年限')
    item.dataIncompleteReason = reasons.join('、')
    item.expiryDate = ''
    item.remainingDays = null
    return
  }

  const expiryParts = calcExpiryDateLocal(purchaseDate, lifeSpan)
  if (!expiryParts) {
    item.dataComplete = false
    item.dataIncompleteReason = '采购日期格式异常'
    item.expiryDate = ''
    item.remainingDays = null
    return
  }

  const expiryStr = formatDateParts(expiryParts)
  const remaining = calcRemainingDaysLocal(expiryStr)

  item.expiryDate = expiryStr
  item.remainingDays = remaining
  item.dataComplete = true
  item.dataIncompleteReason = null
}

const loading = ref(false)
const labs = ref([])
const incompleteCount = ref(0)

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
  labId: null,
  status: '',
  expiredOnly: false,
  includeIncomplete: true,
  sortBy: 'remainingDays',
  sortOrder: 'asc'
})

const hasActiveFilters = computed(() => {
  return searchForm.labId !== null || searchForm.status !== '' || searchForm.expiredOnly || !searchForm.includeIncomplete
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

const getRemainingDaysType = (days) => {
  if (days < 0) return 'danger'
  if (days <= 7) return 'danger'
  if (days <= 14) return 'warning'
  return 'success'
}

const getRemainingDaysText = (days) => {
  if (days < 0) return `已超期${Math.abs(days)}天`
  if (days === 0) return '今天到期'
  return `剩余${days}天`
}

const rowClassName = ({ row }) => {
  if (!row.dataComplete) return 'incomplete-row'
  if (row.remainingDays < 0) return 'expired-row'
  return ''
}

const fetchExpiringEquipments = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.pageSize,
      expiredOnly: searchForm.expiredOnly,
      includeIncomplete: searchForm.includeIncomplete,
      sortBy: searchForm.sortBy,
      sortOrder: searchForm.sortOrder
    }
    if (searchForm.labId) {
      params.labId = searchForm.labId
    }
    if (searchForm.status) {
      params.status = searchForm.status
    }
    const response = await request.get('/equipments/expiring', { params })
    if (response.content) {
      response.content.forEach(enrichExpiryFields)
    }
    pageData.value = response
    pagination.total = response.totalElements
    incompleteCount.value = (response.content || []).filter(e => !e.dataComplete).length
  } finally {
    loading.value = false
  }
}

const fetchLabs = async () => {
  const params = { page: 1, size: 1000 }
  const response = await request.get('/labs', { params })
  labs.value = response.content
}

const onExpiredOnlyChange = (val) => {
  if (val) {
    searchForm.includeIncomplete = false
  }
  handleSearch()
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchExpiringEquipments()
}

const resetSearch = () => {
  searchForm.labId = null
  searchForm.status = ''
  searchForm.expiredOnly = false
  searchForm.includeIncomplete = true
  searchForm.sortBy = 'remainingDays'
  searchForm.sortOrder = 'asc'
  pagination.currentPage = 1
  fetchExpiringEquipments()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchExpiringEquipments()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchExpiringEquipments()
}

onMounted(() => {
  fetchLabs()
  fetchExpiringEquipments()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.incomplete-text {
  color: #909399;
  font-style: italic;
  cursor: help;
}

:deep(.incomplete-row) {
  background-color: #fdf6ec;
}

:deep(.expired-row) {
  background-color: #fef0f0;
}
</style>
