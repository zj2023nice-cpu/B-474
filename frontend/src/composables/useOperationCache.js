import { ref } from 'vue'

export function useOperationCache(options = {}) {
  const {
    buildCacheKey = (item, operation) => `${item.id}_${operation}`,
    checkOperation = null
  } = options

  const cache = ref(new Map())

  function get(key) {
    return cache.value.get(key)
  }

  function set(key, value) {
    cache.value.set(key, value)
  }

  function has(key) {
    return cache.value.has(key)
  }

  function clear() {
    cache.value.clear()
  }

  function getOrCheck(item, operation) {
    const key = buildCacheKey(item, operation)
    if (cache.value.has(key)) {
      return cache.value.get(key)
    }
    return null
  }

  async function prefetch(items, operations) {
    if (!checkOperation || !items || items.length === 0 || !operations || operations.length === 0) {
      return
    }

    for (const item of items) {
      for (const op of operations) {
        const key = buildCacheKey(item, op)
        if (!cache.value.has(key)) {
          try {
            const result = await checkOperation(item, op)
            cache.value.set(key, result)
          } catch (e) {
            console.warn(`Failed to check operation ${op} for item ${item.id}:`, e)
          }
        }
      }
    }
  }

  function isOperationAllowed(item, operation, fallback = true) {
    const result = getOrCheck(item, operation)
    if (result != null) {
      return result.allowed
    }
    return fallback
  }

  function getDisabledReason(item, operation, fallback = '当前状态不允许此操作') {
    const result = getOrCheck(item, operation)
    if (result != null) {
      return result.errorMessage || fallback
    }
    return fallback
  }

  return {
    cache,
    get,
    set,
    has,
    clear,
    getOrCheck,
    prefetch,
    isOperationAllowed,
    getDisabledReason
  }
}
