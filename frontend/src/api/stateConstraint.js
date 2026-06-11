import request from './request'
import { BusinessOperation } from '../constants/statusConstants'

export function checkEquipmentOperation(equipmentId, operation) {
  return request.get(`/state-constraints/equipment/${equipmentId}/check`, {
    params: { operation }
  })
}

export function getEquipmentAvailableOperations(equipmentId) {
  return request.get(`/state-constraints/equipment/${equipmentId}/available-operations`)
}

export function checkBorrowOperation(borrowId, operation) {
  return request.get(`/state-constraints/borrow/${borrowId}/check`, {
    params: { operation }
  })
}

export function checkRepairOperation(repairId, operation) {
  return request.get(`/state-constraints/repair/${repairId}/check`, {
    params: { operation }
  })
}

export async function canApplyBorrow(equipmentId) {
  try {
    const result = await checkEquipmentOperation(equipmentId, BusinessOperation.APPLY_BORROW)
    return result.allowed
  } catch (e) {
    return false
  }
}

export async function canReportRepair(equipmentId) {
  try {
    const result = await checkEquipmentOperation(equipmentId, BusinessOperation.REPORT_REPAIR)
    return result.allowed
  } catch (e) {
    return false
  }
}

export async function canApproveBorrow(borrowId) {
  try {
    const result = await checkBorrowOperation(borrowId, BusinessOperation.APPROVE_BORROW)
    return result.allowed
  } catch (e) {
    return false
  }
}

export async function canFinishRepair(repairId) {
  try {
    const result = await checkRepairOperation(repairId, BusinessOperation.FINISH_REPAIR)
    return result.allowed
  } catch (e) {
    return false
  }
}

export function formatStateConstraintError(result) {
  if (!result || result.allowed) return null
  
  let message = result.errorMessage || '操作不允许'
  let details = []
  
  if (result.details && result.details.length > 0) {
    details = result.details
  }
  
  if (result.allowedEquipmentStatusTexts && result.allowedEquipmentStatusTexts.length > 0) {
    details.push(`允许的设备状态：${result.allowedEquipmentStatusTexts.join('、')}`)
  }
  
  return {
    message,
    details,
    errorCode: result.errorCode,
    currentStatus: result.currentEquipmentStatusText
  }
}
