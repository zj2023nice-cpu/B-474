import { ref, readonly } from 'vue'
import { ElMessage } from 'element-plus'

export function useRequest(fetcher, options = {}) {
  const {
    initialData = null,
    onSuccess = null,
    onError = null,
    showErrorMessage = false,
    errorMessage = '操作失败'
  } = options

  const data = ref(initialData)
  const loading = ref(false)
  const error = ref(null)
  const _lock = ref(false)

  async function run(...args) {
    if (_lock.value) return
    _lock.value = true
    loading.value = true
    error.value = null

    try {
      const result = await fetcher(...args)
      data.value = result
      if (onSuccess) onSuccess(result)
      return result
    } catch (e) {
      error.value = e
      if (showErrorMessage) {
        const msg = e?.message || errorMessage
        if (msg && !msg.includes('冲突记录')) {
          ElMessage.error(msg)
        }
      }
      if (onError) onError(e)
      throw e
    } finally {
      loading.value = false
      _lock.value = false
    }
  }

  async function runUnsafe(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await fetcher(...args)
      data.value = result
      if (onSuccess) onSuccess(result)
      return result
    } catch (e) {
      error.value = e
      if (showErrorMessage) {
        const msg = e?.message || errorMessage
        if (msg && !msg.includes('冲突记录')) {
          ElMessage.error(msg)
        }
      }
      if (onError) onError(e)
      throw e
    } finally {
      loading.value = false
    }
  }

  function reset() {
    data.value = initialData
    error.value = null
  }

  return {
    data,
    loading: readonly(loading),
    error: readonly(error),
    locked: readonly(_lock),
    run,
    runUnsafe,
    reset
  }
}

export function useMutation(fetcher, options = {}) {
  const {
    onSuccess = null,
    onError = null,
    errorMessage = '操作失败',
    successMessage = ''
  } = options

  const loading = ref(false)
  const error = ref(null)
  const _lock = ref(false)

  async function mutate(...args) {
    if (_lock.value) return
    _lock.value = true
    loading.value = true
    error.value = null

    try {
      const result = await fetcher(...args)
      if (successMessage) ElMessage.success(successMessage)
      if (onSuccess) onSuccess(result)
      return result
    } catch (e) {
      error.value = e
      const msg = e?.message || errorMessage
      if (msg && !msg.includes('冲突记录')) {
        ElMessage.error(msg)
      }
      if (onError) onError(e)
      throw e
    } finally {
      loading.value = false
      _lock.value = false
    }
  }

  return {
    loading: readonly(loading),
    error: readonly(error),
    locked: readonly(_lock),
    mutate
  }
}
