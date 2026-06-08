<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备到期提醒</span>
          <el-tag type="warning">30天内即将到期的设备</el-tag>
        </div>
      </template>

      <el-table :data="expiringEquipments" style="width: 100%" v-loading="loading">
        <el-table-column prop="code" label="设备编号" width="120" />
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="model" label="型号" />
        <el-table-column prop="lab.name" label="所属实验室" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="采购日期" width="120" />
        <el-table-column prop="lifeSpan" label="使用年限" width="100">
          <template #default="scope">
            {{ scope.row.lifeSpan }}年
          </template>
        </el-table-column>
        <el-table-column label="到期日期" width="120">
          <template #default="scope">
            {{ getExpiryDate(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="120">
          <template #default="scope">
            <el-tag :type="getRemainingDaysType(getRemainingDays(scope.row))">
              {{ getRemainingDaysText(getRemainingDays(scope.row)) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && expiringEquipments.length === 0" description="暂无即将到期的设备" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'

const loading = ref(false)
const expiringEquipments = ref([])

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

const getExpiryDate = (equipment) => {
  if (!equipment.purchaseDate || !equipment.lifeSpan) return '-'
  const purchaseDate = new Date(equipment.purchaseDate)
  purchaseDate.setFullYear(purchaseDate.getFullYear() + equipment.lifeSpan)
  return purchaseDate.toISOString().split('T')[0]
}

const getRemainingDays = (equipment) => {
  if (!equipment.purchaseDate || !equipment.lifeSpan) return 0
  const purchaseDate = new Date(equipment.purchaseDate)
  const expiryDate = new Date(purchaseDate)
  expiryDate.setFullYear(expiryDate.getFullYear() + equipment.lifeSpan)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const diffTime = expiryDate - today
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays
}

const getRemainingDaysType = (days) => {
  if (days <= 7) return 'danger'
  if (days <= 14) return 'warning'
  return 'success'
}

const getRemainingDaysText = (days) => {
  if (days < 0) return `已超期${Math.abs(days)}天`
  if (days === 0) return '今天到期'
  return `剩余${days}天`
}

const fetchExpiringEquipments = async () => {
  loading.value = true
  try {
    const response = await request.get('/equipments/expiring')
    expiringEquipments.value = response
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchExpiringEquipments()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
