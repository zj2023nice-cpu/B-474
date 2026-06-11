<template>
  <div>
    <el-row :gutter="20" v-loading="statsReq.loading.value">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>设备总数</template>
          <div class="stat-value">{{ stats.equipmentCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>当前借用</template>
          <div class="stat-value">{{ stats.borrowCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>待维修</template>
          <div class="stat-value">{{ stats.repairCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;" v-loading="statsReq.loading.value">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>待审批借用</template>
          <div class="stat-value warning">{{ stats.pendingBorrowCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>近 7 天新增报修</template>
          <div class="stat-value danger">{{ stats.recentRepairCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px;" v-loading="statsReq.loading.value">
      <template #header>提醒</template>
      <template v-if="statsReq.error.value">
        <el-alert :title="statsReq.error.value?.message || '加载失败，请刷新重试'" type="error" show-icon :closable="false" />
      </template>
      <template v-else>
        <el-alert
          v-if="stats.overdue > 0"
          :title="`${stats.overdue} 台设备即将超期或已超期！`"
          type="error"
          show-icon
          :closable="false"
        />
        <div v-else>暂无提醒。</div>
      </template>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>最近待处理事项</template>
      <div v-loading="pendingReq.loading.value">
        <template v-if="pendingReq.error.value">
          <el-alert :title="pendingReq.error.value?.message || '加载失败'" type="error" show-icon :closable="false" />
        </template>
        <el-table :data="pendingItems" stripe style="width: 100%" v-else-if="pendingItems.length > 0">
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'BORROW' ? 'primary' : 'warning'" size="small">
                {{ row.type === 'BORROW' ? '借用' : '报修' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="equipmentName" label="设备名称" />
          <el-table-column prop="userName" label="申请人/报修人" width="140" />
          <el-table-column prop="description" label="说明" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row)" size="small">{{ statusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="180" />
        </el-table>
        <el-empty v-else description="暂无待处理事项" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../api/request'
import { useRequest } from '../composables/useRequest'

const stats = ref({
  equipmentCount: 0,
  borrowCount: 0,
  repairCount: 0,
  overdue: 0,
  pendingBorrowCount: 0,
  recentRepairCount: 0
})

const pendingItems = ref([])

const statusMap = {
  PENDING: '待审批',
  APPROVED: '已批准',
  RETURNED: '已归还',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
  REPORTED: '已报修',
  IN_PROGRESS: '维修中',
  FINISHED: '已完成'
}

const statusTagTypeMap = {
  PENDING: 'warning',
  APPROVED: 'success',
  RETURNED: 'info',
  REJECTED: 'danger',
  CANCELLED: 'info',
  REPORTED: 'danger',
  IN_PROGRESS: 'warning',
  FINISHED: 'success'
}

function statusLabel(row) {
  return statusMap[row.status] || row.status
}

function statusTagType(row) {
  return statusTagTypeMap[row.status] || 'info'
}

const statsReq = useRequest(() => request.get('/stats'), {
  showErrorMessage: true,
  errorMessage: '统计数据加载失败',
  onSuccess: (data) => {
    stats.value.equipmentCount = data?.equipmentCount ?? 0
    stats.value.borrowCount = data?.borrowCount ?? 0
    stats.value.overdue = data?.overdue ?? 0
    stats.value.repairCount = data?.repairCount ?? 0
    stats.value.pendingBorrowCount = data?.pendingBorrowCount ?? 0
    stats.value.recentRepairCount = data?.recentRepairCount ?? 0
  }
})

const pendingReq = useRequest(() => request.get('/stats/pending-items'), {
  initialData: [],
  showErrorMessage: true,
  errorMessage: '待处理事项加载失败',
  onSuccess: (data) => {
    pendingItems.value = data || []
  }
})

onMounted(async () => {
  await Promise.allSettled([
    statsReq.run(),
    pendingReq.run()
  ])
})
</script>

<style scoped>
.stat-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  color: #409EFF;
}
.stat-value.warning {
  color: #E6A23C;
}
.stat-value.danger {
  color: #F56C6C;
}
</style>
