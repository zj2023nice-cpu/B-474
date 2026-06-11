import { ROLE_PERMISSIONS, PERMISSIONS } from '../constants/roleConstants'

export function hasRole(userRole, roles) {
  if (!userRole) return false
  if (!roles || roles.length === 0) return true
  return roles.includes(userRole)
}

export function hasPermission(userRole, permission) {
  if (!userRole) return false
  const permissions = ROLE_PERMISSIONS[userRole] || []
  return permissions.includes(permission)
}

export function hasAnyPermission(userRole, permissions) {
  if (!userRole) return false
  if (!permissions || permissions.length === 0) return true
  return permissions.some(p => hasPermission(userRole, p))
}

export function hasAllPermissions(userRole, permissions) {
  if (!userRole) return false
  if (!permissions || permissions.length === 0) return true
  return permissions.every(p => hasPermission(userRole, p))
}

export { PERMISSIONS }
