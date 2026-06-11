<template>
  <div>
    <div class="header-actions">
      <el-button v-if="canCreateLab" type="primary" @click="showAddDialog" :loading="saveMutation.loading.value" :disabled="saveMutation.locked.value">新增实验室</el-button>
      <el-input v-model="listPage.searchForm.name" placeholder="搜索名称" style="width: 150px; margin-left: 10px" clearable @clear="listPage.handleSearch" />
      <el-input v-model="listPage.searchForm.building" placeholder="搜索楼宇" style="width: 150px; margin-left: 10px" clearable @clear="listPage.handleSearch" />
      <el-input v-model="listPage.searchForm.picName" placeholder="搜索负责人" style="width: 150px; margin-left: 10px" clearable @clear="listPage.handleSearch" />
      <el-button type="primary" style="margin-left: 10px" @click="listPage.handleSearch" :loading="listPage.loading.value">搜索</el-button>
      <el-button style="margin-left: 10px" @click="listPage.resetSearch" :disabled="listPage.loading.value">重置</el-button>
    </div>
    
    <el-table :data="listPage.pageData.content" style="width: 100%" v-loading="listPage.loading.value">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="building" label="楼宇" />
      <el-table-column prop="room" label="房间号" />
      <el-table-column prop="picName" label="负责人" />
      <el-table-column prop="picPhone" label="电话" />
      <el-table-column prop="capacity" label="容量" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" size="small" @click="openDetail(scope.row)">详情</el-button>
          <el-button v-if="canEditLab" type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
            v-if="canDeleteLab"
            type="danger"
            size="small"
            @click="handleDelete(scope.row)"
            :loading="deleteMutation.loading.value && deleteRowId.value === scope.row.id"
            :disabled="deleteMutation.locked.value"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="listPage.isEmpty.value" description="暂无实验室数据" />

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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑实验室' : '新增实验室'">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="楼宇">
          <el-input v-model="form.building" />
        </el-form-item>
        <el-form-item label="房间号">
          <el-input v-model="form.room" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.picName" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.picPhone" />
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="form.capacity" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <LabDetailDrawer v-model="drawerVisible" :lab-id="selectedLabId" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import LabDetailDrawer from '../components/LabDetailDrawer.vue'
import { PERMISSIONS } from '../constants/roleConstants'
import { useListPage } from '../composables/useListPage'
import { useMutation } from '../composables/useRequest'

const userStore = useUserStore()
const canCreateLab = computed(() => userStore.hasPermission(PERMISSIONS.LAB_CREATE))
const canEditLab = computed(() => userStore.hasPermission(PERMISSIONS.LAB_EDIT))
const canDeleteLab = computed(() => userStore.hasPermission(PERMISSIONS.LAB_DELETE))

const dialogVisible = ref(false)
const form = ref({})

const drawerVisible = ref(false)
const selectedLabId = ref(null)
const deleteRowId = ref(null)

const listPage = useListPage({
  apiPath: '/labs',
  initialSearchForm: {
    name: '',
    building: '',
    picName: ''
  }
})

const saveMutation = useMutation(
  (payload) => {
    if (payload.id) {
      return request.put(`/labs/${payload.id}`, payload)
    }
    return request.post('/labs', payload)
  },
  {
    successMessage: '实验室已保存',
    errorMessage: '保存失败',
    onSuccess: (result, payload) => {
      dialogVisible.value = false
      if (payload.id) {
        listPage.updateRow(payload.id, result)
      } else {
        listPage.refresh()
      }
    }
  }
)

const deleteMutation = useMutation(
  (id) => request.delete(`/labs/${id}`),
  {
    successMessage: '已删除',
    errorMessage: '删除失败',
    onSuccess: (_, id) => {
      listPage.removeRow(id)
    }
  }
)

const openDetail = (row) => {
  selectedLabId.value = row.id
  drawerVisible.value = true
}

const showAddDialog = () => {
  form.value = { capacity: 0 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await saveMutation.mutate(form.value)
  } catch (e) {}
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    deleteRowId.value = row.id
    try {
      await deleteMutation.mutate(row.id)
    } catch (e) {}
    finally {
      deleteRowId.value = null
    }
  }).catch(() => {
    deleteRowId.value = null
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
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
