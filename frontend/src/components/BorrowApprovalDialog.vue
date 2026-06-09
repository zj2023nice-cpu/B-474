<template>
  <el-dialog v-model="visible" :title="title" width="480px" @close="handleClose">
    <el-form :model="form" label-width="90px">
      <el-form-item label="申请人">
        <span>{{ borrowRecord?.applicant?.name }}</span>
      </el-form-item>
      <el-form-item label="设备">
        <span>{{ borrowRecord?.equipment?.name }}</span>
      </el-form-item>
      <el-form-item label="借用时间">
        <span>{{ borrowRecord?.startTime }} ~ {{ borrowRecord?.endTime }}</span>
      </el-form-item>
      <el-form-item v-if="action === 'reject'" label="拒绝原因" required>
        <el-input
          v-model="form.rejectReason"
          type="textarea"
          :rows="3"
          placeholder="请填写拒绝原因"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button :type="action === 'approve' ? 'success' : 'danger'" @click="handleConfirm">
        {{ action === 'approve' ? '确认批准' : '确认拒绝' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'

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

const form = reactive({
  rejectReason: ''
})

const handleClose = () => {
  form.rejectReason = ''
  visible.value = false
}

const handleConfirm = () => {
  if (props.action === 'reject' && !form.rejectReason.trim()) {
    return
  }
  emit('confirm', {
    action: props.action,
    rejectReason: form.rejectReason
  })
  handleClose()
}
</script>
