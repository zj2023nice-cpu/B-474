<template>
  <div>
    <el-row :gutter="20">
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

    <el-card style="margin-top: 20px;">
      <template #header>提醒</template>
      <el-alert
        v-if="stats.overdue > 0"
        :title="`${stats.overdue} 台设备即将超期或已超期！`"
        type="error"
        show-icon
        :closable="false"
      />
      <div v-else>暂无提醒。</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'

const stats = ref({
  equipmentCount: 0,
  borrowCount: 0,
  repairCount: 0,
  overdue: 0
})

onMounted(async () => {
  try {
    const data = await request.get('/stats')
    stats.value.equipmentCount = data.equipmentCount || 0
    stats.value.borrowCount = data.borrowCount || 0
    stats.value.overdue = data.overdue || 0
    stats.value.repairCount = data.repairCount || 0
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.stat-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  color: #409EFF;
}
</style>
