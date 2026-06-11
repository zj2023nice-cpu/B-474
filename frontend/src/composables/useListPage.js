import { ref, reactive, computed, readonly } from 'vue'
import request from '../api/request'

export function useListPage(config = {}) {
  const {
    apiPath,
    buildParams = null,
    initialSearchForm = {},
    pageSize = 10,
    autoFetch = true,
    onDataLoaded = null,
    enablePageFallback = true,
    maxFallbackAttempts = 5
  } = config

  const pageData = ref({
    content: [],
    totalPages: 0,
    totalElements: 0,
    currentPage: 1,
    pageSize: pageSize,
    hasNext: false,
    hasPrevious: false
  })

  const pagination = reactive({
    currentPage: 1,
    pageSize: pageSize,
    total: 0
  })

  const searchForm = reactive({ ...initialSearchForm })
  const _initialSearchSnapshot = { ...initialSearchForm }

  const loading = ref(false)
  const error = ref(null)
  const _fetchLock = ref(false)
  const _requestId = ref(0)

  const isEmpty = computed(() => {
    return !loading.value && (!pageData.value.content || pageData.value.content.length === 0)
  })

  const hasError = computed(() => !!error.value)

  function _resetSearchForm() {
    Object.keys(_initialSearchSnapshot).forEach(key => {
      searchForm[key] = _initialSearchSnapshot[key]
    })
  }

  function _buildFetchParams() {
    const baseParams = {
      page: pagination.currentPage,
      size: pagination.pageSize
    }

    if (buildParams) {
      const extra = buildParams(searchForm) || {}
      return { ...baseParams, ...extra }
    }

    Object.keys(searchForm).forEach(key => {
      const val = searchForm[key]
      if (val !== '' && val !== null && val !== undefined) {
        baseParams[key] = val
      }
    })

    return baseParams
  }

  async function _doFetch() {
    if (_fetchLock.value) return
    _fetchLock.value = true
    loading.value = true
    error.value = null

    const myRequestId = ++_requestId.value

    try {
      const params = _buildFetchParams()
      const response = await request.get(apiPath, { params })

      if (_requestId.value !== myRequestId) return

      pageData.value = response
      pagination.total = response.totalElements

      if (onDataLoaded) {
        await onDataLoaded(response.content || [])
      }
    } catch (e) {
      if (_requestId.value !== myRequestId) return
      error.value = e
      throw e
    } finally {
      if (_requestId.value === myRequestId) {
        loading.value = false
        _fetchLock.value = false
      }
    }
  }

  async function _doFetchWithFallback() {
    if (!enablePageFallback) {
      return _doFetch()
    }

    let attempts = 0

    while (attempts < maxFallbackAttempts) {
      attempts++
      await _doFetch()

      const content = pageData.value.content
      const totalPages = pageData.value.totalPages
      const contentEmpty = !content || content.length === 0
      const currentPage = pagination.currentPage

      if (!contentEmpty) break

      if (totalPages <= 0) {
        if (currentPage !== 1) {
          pagination.currentPage = 1
        } else {
          break
        }
      } else if (currentPage > totalPages) {
        pagination.currentPage = totalPages
      } else if (currentPage > 1) {
        pagination.currentPage = currentPage - 1
      } else {
        break
      }
    }
  }

  function fetch() {
    return _doFetch()
  }

  function refresh() {
    return _doFetchWithFallback()
  }

  function handleSearch() {
    pagination.currentPage = 1
    return _doFetch()
  }

  function resetSearch() {
    _resetSearchForm()
    pagination.currentPage = 1
    return _doFetch()
  }

  function handleSizeChange(size) {
    pagination.pageSize = size
    pagination.currentPage = 1
    return _doFetch()
  }

  function handleCurrentChange(page) {
    pagination.currentPage = page
    return _doFetch()
  }

  if (autoFetch) {
    _doFetch()
  }

  return {
    pageData,
    pagination,
    searchForm,
    loading: readonly(loading),
    error: readonly(error),
    isEmpty,
    hasError,
    fetch,
    refresh,
    handleSearch,
    resetSearch,
    handleSizeChange,
    handleCurrentChange
  }
}
