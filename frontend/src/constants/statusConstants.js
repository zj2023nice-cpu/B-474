export const EquipmentStatus = {
  NORMAL: 'NORMAL',
  BORROWED: 'BORROWED',
  REPAIRING: 'REPAIRING',
  SCRAPPED: 'SCRAPPED'
}

export const EquipmentStatusText = {
  [EquipmentStatus.NORMAL]: '正常',
  [EquipmentStatus.BORROWED]: '借用中',
  [EquipmentStatus.REPAIRING]: '维修中',
  [EquipmentStatus.SCRAPPED]: '报废'
}

export const EquipmentStatusType = {
  [EquipmentStatus.NORMAL]: 'success',
  [EquipmentStatus.BORROWED]: 'warning',
  [EquipmentStatus.REPAIRING]: 'danger',
  [EquipmentStatus.SCRAPPED]: 'info'
}

export const BorrowStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  RETURNED: 'RETURNED',
  CANCELLED: 'CANCELLED'
}

export const BorrowStatusText = {
  [BorrowStatus.PENDING]: '待审批',
  [BorrowStatus.APPROVED]: '已批准',
  [BorrowStatus.REJECTED]: '已拒绝',
  [BorrowStatus.RETURNED]: '已归还',
  [BorrowStatus.CANCELLED]: '已取消'
}

export const BorrowStatusType = {
  [BorrowStatus.PENDING]: 'warning',
  [BorrowStatus.APPROVED]: 'success',
  [BorrowStatus.REJECTED]: 'danger',
  [BorrowStatus.RETURNED]: 'info',
  [BorrowStatus.CANCELLED]: 'info'
}

export const RepairStatus = {
  REPORTED: 'REPORTED',
  IN_PROGRESS: 'IN_PROGRESS',
  FINISHED: 'FINISHED'
}

export const RepairStatusText = {
  [RepairStatus.REPORTED]: '已上报',
  [RepairStatus.IN_PROGRESS]: '维修中',
  [RepairStatus.FINISHED]: '已完成'
}

export const RepairStatusType = {
  [RepairStatus.REPORTED]: 'danger',
  [RepairStatus.IN_PROGRESS]: 'warning',
  [RepairStatus.FINISHED]: 'success'
}

export const BusinessOperation = {
  APPLY_BORROW: 'APPLY_BORROW',
  APPROVE_BORROW: 'APPROVE_BORROW',
  REPORT_REPAIR: 'REPORT_REPAIR',
  FINISH_REPAIR: 'FINISH_REPAIR',
  RETURN_EQUIPMENT: 'RETURN_EQUIPMENT',
  CANCEL_BORROW: 'CANCEL_BORROW',
  CANCEL_REPAIR: 'CANCEL_REPAIR'
}

export const BusinessOperationText = {
  [BusinessOperation.APPLY_BORROW]: '申请借用',
  [BusinessOperation.APPROVE_BORROW]: '批准借用',
  [BusinessOperation.REPORT_REPAIR]: '申请报修',
  [BusinessOperation.FINISH_REPAIR]: '完成维修',
  [BusinessOperation.RETURN_EQUIPMENT]: '归还设备',
  [BusinessOperation.CANCEL_BORROW]: '取消借用',
  [BusinessOperation.CANCEL_REPAIR]: '取消报修'
}

export const ErrorCode = {
  EQUIPMENT_STATUS_NOT_ALLOWED: 'EQUIPMENT_STATUS_NOT_ALLOWED',
  HAS_ACTIVE_REPAIR: 'HAS_ACTIVE_REPAIR',
  HAS_ACTIVE_BORROW: 'HAS_ACTIVE_BORROW',
  BORROW_STATUS_NOT_ALLOWED: 'BORROW_STATUS_NOT_ALLOWED',
  REPAIR_STATUS_NOT_ALLOWED: 'REPAIR_STATUS_NOT_ALLOWED'
}

export function getEquipmentStatusText(status) {
  return EquipmentStatusText[status] || status || '-'
}

export function getEquipmentStatusType(status) {
  return EquipmentStatusType[status] || 'info'
}

export function getBorrowStatusText(status) {
  return BorrowStatusText[status] || status || '-'
}

export function getBorrowStatusType(status) {
  return BorrowStatusType[status] || 'info'
}

export function getRepairStatusText(status) {
  return RepairStatusText[status] || status || '-'
}

export function getRepairStatusType(status) {
  return RepairStatusType[status] || 'info'
}

export function canBorrowEquipment(equipment) {
  if (!equipment || !equipment.status) return false
  return equipment.status === EquipmentStatus.NORMAL
}

export function canRepairEquipment(equipment) {
  if (!equipment || !equipment.status) return false
  return equipment.status === EquipmentStatus.NORMAL || equipment.status === EquipmentStatus.BORROWED
}

export function getEquipmentDisabledReason(equipment, operation) {
  if (!equipment) return '设备不存在'
  const status = equipment.status
  const statusText = getEquipmentStatusText(status)
  
  if (operation === BusinessOperation.APPLY_BORROW) {
    if (!canBorrowEquipment(equipment)) {
      return `设备当前状态为「${statusText}」，仅「正常」状态的设备可借用`
    }
  } else if (operation === BusinessOperation.REPORT_REPAIR) {
    if (!canRepairEquipment(equipment)) {
      return `设备当前状态为「${statusText}」，仅「正常」或「借用中」状态的设备可报修`
    }
  }
  return null
}
