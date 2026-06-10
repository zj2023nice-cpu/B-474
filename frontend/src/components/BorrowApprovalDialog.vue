<template>
  <el-dialog v-model="visible" :title="title" width="520px" @close="handleClose" :close-on-click-modal="false">
    <div class="approval-summary">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="申请人">{{ borrowRecord?.applicant?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ borrowRecord?.equipment?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="借用时间">{{ borrowRecord?.startTime || '-' }} ~ {{ borrowRecord?.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途">{{ borrowRecord?.purpose || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-if="action === 'approve'" class="approval-confirm">
      <el-alert type="success" :closable="false" show-icon>
        <template #title>确认批准该借用申请？</template>
        <template #default>批准后设备状态将变更为「已借出」，请确认设备可借用。</template>
      </el-alert>
    </div>

    <div v-if="action === 'reject'" class="approval-confirm">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>拒绝该借用申请</template>
        <template #default>请填写拒绝原因，该原因将通知申请人。</template>
      </el-alert>
      <el-form ref="rejectFormRef" :model="form" :rules="rejectRules" label-width="90px" style="margin-top: 16px">
        <el-form-item label="拒绝原因" prop="rejectReason">
          <el-input
            v-model="form.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请填写拒绝原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button v-if="action === 'approve'" type="success" :loading="submitting" @click="handleConfirm">
        确认批准
      </el-button>
      <el-button v-if="action === 'reject'" type="danger" :loading="submitting" @click="handleConfirm">
        确认拒绝
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive, watch } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  action: String,
  borrowRecord: Object
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const title = computed(() => props.action === 'approve' ? '批准借用申请' : '拒绝借用申请')

const rejectFormRef = ref(null)
const submitting = ref(false)

const form = reactive({
  rejectReason: ''
})

const rejectRules = {
  rejectReason: [
    { required: true, message: '拒绝原因不能为空', trigger: 'blur' },
    { min: 2, message: '拒绝原因至少输入 2 个字符', trigger: 'blur' },
    { max: 500, message: '拒绝原因不能超过 500 个字符', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (val) => {
  if (!val) {
    form.rejectReason = ''
  }
})

const handleClose = () => {
  form.rejectReason = ''
  visible.value = false
}

const handleConfirm = async () => {
  if (props.action === 'reject') {
    try {
      await rejectFormRef.value.validate()
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    emit('confirm', {
      action: props.action,
      rejectReason: form.rejectReason
    })
  } finally {
    submitting.value = false
  }
  handleClose()
}
</script>

<style scoped>
.approval-summary {
  margin-bottom: 16px;
}

.approval-confirm {
  margin-top: 8px;
}
</style>
