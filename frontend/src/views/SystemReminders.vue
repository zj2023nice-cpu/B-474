<template>
  <div class="reminders-page">
    <el-card class="summary-card" shadow="never">
      <div class="summary-inner">
        <div class="summary-left">
          <div class="summary-title">
            <el-icon :size="24" color="#409EFF"><Bell /></el-icon>
            <span>系统提醒中心</span>
          </div>
          <div class="summary-subtitle">
            聚合所有待处理事项，按优先级排序
            <span v-if="reminders.generatedAt" class="generated-time">
              · 生成于 {{ formatTime(reminders.generatedAt) }}
            </span>
          </div>
        </div>
        <div class="summary-right">
          <div class="stat-block" v-if="reminders.totalCount > 0">
            <div class="stat-num">{{ reminders.totalCount }}</div>
            <div class="stat-label">待处理总数</div>
          </div>
          <div class="stat-block danger" v-if="reminders.highPriorityCount > 0">
            <div class="stat-num">{{ reminders.highPriorityCount }}</div>
            <div class="stat-label">高优先级</div>
          </div>
          <div class="stat-block empty" v-else-if="reminders.totalCount === 0">
            <div class="stat-num"><el-icon :size="20"><CircleCheck /></el-icon></div>
            <div class="stat-label">全部处理完毕</div>
          </div>
        </div>
      </div>
    </el-card>

    <div v-loading="loading" class="modules-container">
      <div
        v-for="module in reminders.modules"
        :key="module.key"
        class="module-card"
      >
        <el-card shadow="hover">
          <template #header>
            <div class="module-header">
              <div class="module-header-left">
                <el-tag
                  :type="getOverallPriorityTagType(module.overallPriority)"
                  size="small"
                  effect="dark"
                  class="priority-tag"
                >
                  {{ getPriorityLabel(module.overallPriority) }}
                </el-tag>
                <span class="module-title">{{ module.title }}</span>
                <el-tag size="small" class="count-tag" v-if="module.totalCount > 0">
                  {{ module.totalCount }} 项
                </el-tag>
              </div>
              <div class="module-header-right">
                <span class="module-desc">{{ module.description }}</span>
                <el-button
                  type="primary"
                  link
                  @click="goToModule(module.actionRoute)"
                  v-if="module.totalCount > 0"
                >
                  查看全部
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="module.items && module.items.length > 0" class="module-items">
            <div
              v-for="item in module.items.slice(0, displayLimit)"
              :key="module.key + '-' + item.id"
              class="reminder-item"
              :class="'priority-' + (item.priority || 'LOW').toLowerCase()"
              @click="handleItemClick(module, item)"
            >
              <div class="item-left">
                <el-tag
                  :type="getPriorityTagType(item.priority)"
                  size="small"
                  effect="plain"
                  class="item-priority-tag"
                >
                  {{ getPriorityLabel(item.priority) }}
                </el-tag>
              </div>
              <div class="item-main">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-subtitle">{{ item.subtitle }}</div>
                <div class="item-extra" v-if="hasExtraInfo(module.key, item)">
                  <template v-if="module.key === 'overdue_borrows'">
                    <span v-if="item.extra?.purpose" class="extra-chip">
                      <el-icon><Document /></el-icon>
                      {{ truncate(item.extra.purpose, 30) }}
                    </span>
                  </template>
                  <template v-else-if="module.key === 'expiring_equipments'">
                    <span v-if="item.extra?.model" class="extra-chip">
                      <el-icon><Cpu /></el-icon>
                      {{ item.extra.model }}
                    </span>
                    <span v-if="item.extra?.manufacturer" class="extra-chip">
                      <el-icon><OfficeBuilding /></el-icon>
                      {{ item.extra.manufacturer }}
                    </span>
                  </template>
                  <template v-else-if="module.key === 'unfinished_repairs'">
                    <span v-if="item.extra?.description" class="extra-chip">
                      <el-icon><Tools /></el-icon>
                      {{ truncate(item.extra.description, 30) }}
                    </span>
                    <span v-if="item.extra?.repairCompany" class="extra-chip">
                      <el-icon><Van /></el-icon>
                      {{ item.extra.repairCompany }}
                    </span>
                  </template>
                </div>
              </div>
              <div class="item-right">
                <div class="item-time-label">{{ getTimeLabel(module.key) }}</div>
                <div class="item-time">{{ formatShortTime(item.time) }}</div>
              </div>
            </div>

            <div v-if="module.items.length > displayLimit" class="more-hint">
              还有 {{ module.items.length - displayLimit }} 项未展示，
              <el-button type="primary" link @click="goToModule(module.actionRoute)">
                点击查看全部
              </el-button>
            </div>
          </div>

          <div v-else class="module-empty">
            <el-empty
              :description="module.emptyStateTitle"
              :image-size="80"
            >
              <template #description>
                <div class="empty-title">{{ module.emptyStateTitle }}</div>
                <div class="empty-desc">{{ module.emptyStateDescription }}</div>
              </template>
            </el-empty>
          </div>
        </el-card>
      </div>

      <div v-if="!loading && reminders.modules && reminders.modules.length === 0" class="global-empty">
        <el-empty description="暂无提醒模块">
          <template #description>
            <div class="empty-title">暂无待处理的提醒</div>
            <div class="empty-desc">所有模块都运行正常，继续保持！</div>
          </template>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Bell, ArrowRight, CircleCheck, Document, Cpu, OfficeBuilding, Tools, Van
} from '@element-plus/icons-vue'
import request from '../api/request'

const router = useRouter()
const loading = ref(false)
const displayLimit = 5

const reminders = reactive({
  totalCount: 0,
  highPriorityCount: 0,
  generatedAt: null,
  modules: []
})

const priorityTagTypeMap = {
  HIGH: 'danger',
  MEDIUM: 'warning',
  LOW: 'info'
}

const priorityLabelMap = {
  HIGH: '高优先级',
  MEDIUM: '中优先级',
  LOW: '低优先级'
}

function getPriorityTagType(priority) {
  return priorityTagTypeMap[priority] || 'info'
}

function getOverallPriorityTagType(priority) {
  return priorityTagTypeMap[priority] || 'success'
}

function getPriorityLabel(priority) {
  return priorityLabelMap[priority] || '普通'
}

function getTimeLabel(moduleKey) {
  if (moduleKey === 'overdue_borrows') return '到期时间'
  if (moduleKey === 'expiring_equipments') return '到期日期'
  if (moduleKey === 'unfinished_repairs') return '报修时间'
  return '时间'
}

function hasExtraInfo(moduleKey, item) {
  if (!item.extra) return false
  if (moduleKey === 'overdue_borrows') return !!item.extra.purpose
  if (moduleKey === 'expiring_equipments') return !!(item.extra.model || item.extra.manufacturer)
  if (moduleKey === 'unfinished_repairs') return !!(item.extra.description || item.extra.repairCompany)
  return false
}

function truncate(str, max) {
  if (!str) return ''
  return str.length > max ? str.slice(0, max) + '…' : str
}

function formatTime(str) {
  if (!str) return ''
  const d = new Date(str)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatShortTime(str) {
  if (!str) return '-'
  const d = new Date(str)
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const sameDay = d.getFullYear() === now.getFullYear()
    && d.getMonth() === now.getMonth()
    && d.getDate() === now.getDate()
  if (sameDay) {
    return `今天 ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function goToModule(route) {
  if (route) {
    router.push(route)
  }
}

function handleItemClick(module, item) {
  if (module.actionRoute) {
    router.push(module.actionRoute)
  }
}

async function fetchReminders() {
  loading.value = true
  try {
    const data = await request.get('/reminders')
    reminders.totalCount = data.totalCount || 0
    reminders.highPriorityCount = data.highPriorityCount || 0
    reminders.generatedAt = data.generatedAt
    reminders.modules = data.modules || []
  } catch (e) {
    console.error(e)
    ElMessage.error('获取提醒数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchReminders()
})
</script>

<style scoped>
.reminders-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-card {
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
  border: none;
}

.summary-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.summary-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.generated-time {
  color: #c0c4cc;
}

.summary-right {
  display: flex;
  gap: 32px;
}

.stat-block {
  text-align: center;
  min-width: 80px;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #409EFF;
  line-height: 1.2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-block.danger .stat-num {
  color: #F56C6C;
}

.stat-block.empty .stat-num {
  color: #67C23A;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.modules-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.module-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.priority-tag {
  font-weight: 500;
}

.module-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.count-tag {
  background-color: #f4f4f5;
  color: #606266;
  border-color: #e9e9eb;
}

.module-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.module-desc {
  font-size: 12px;
  color: #909399;
}

.module-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reminder-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background-color: #fafafa;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reminder-item:hover {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
  transform: translateX(2px);
}

.reminder-item.priority-high {
  border-left: 4px solid #F56C6C;
  background-color: #fef0f0;
}

.reminder-item.priority-high:hover {
  background-color: #fde2e2;
}

.reminder-item.priority-medium {
  border-left: 4px solid #E6A23C;
  background-color: #fdf6ec;
}

.reminder-item.priority-medium:hover {
  background-color: #faecd8;
}

.reminder-item.priority-low {
  border-left: 4px solid #909399;
}

.item-left {
  flex-shrink: 0;
  padding-top: 2px;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
}

.item-subtitle {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
  line-height: 1.4;
}

.item-extra {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.extra-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #909399;
  background-color: #f4f4f5;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1.6;
}

.extra-chip .el-icon {
  font-size: 11px;
}

.item-right {
  flex-shrink: 0;
  text-align: right;
  min-width: 90px;
}

.item-time-label {
  font-size: 11px;
  color: #c0c4cc;
}

.item-time {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
  font-variant-numeric: tabular-nums;
}

.more-hint {
  text-align: center;
  padding: 10px;
  font-size: 12px;
  color: #909399;
  border-top: 1px dashed #ebeef5;
  margin-top: 8px;
}

.module-empty {
  padding: 20px 0;
}

.empty-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.empty-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.global-empty {
  padding: 40px 0;
}
</style>
