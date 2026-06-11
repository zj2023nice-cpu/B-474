import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ROLES } from '../constants/roleConstants'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { hidden: true }
    },
    {
      path: '/',
      component: () => import('../views/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/Dashboard.vue'),
          meta: { title: '首页', icon: 'Odometer' }
        },
        {
          path: 'labs',
          name: 'labs',
          component: () => import('../views/LabList.vue'),
          meta: { title: '实验室管理', icon: 'OfficeBuilding' }
        },
        {
          path: 'equipments',
          name: 'equipments',
          component: () => import('../views/EquipmentList.vue'),
          meta: { title: '设备管理', icon: 'Monitor' }
        },
        {
          path: 'borrows',
          name: 'borrows',
          component: () => import('../views/BorrowList.vue'),
          meta: { title: '借用管理', icon: 'List' }
        },
        {
          path: 'repairs',
          name: 'repairs',
          component: () => import('../views/RepairList.vue'),
          meta: { title: '维修管理', icon: 'Tools' }
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('../views/UserList.vue'),
          meta: { title: '用户管理', icon: 'User', roles: [ROLES.ADMIN] }
        },
        {
          path: 'expiring',
          name: 'expiring',
          component: () => import('../views/ExpiringEquipments.vue'),
          meta: { title: '到期提醒', icon: 'AlarmClock', roles: [ROLES.ADMIN, ROLES.TEACHER] }
        },
        {
          path: 'reminders',
          name: 'reminders',
          component: () => import('../views/SystemReminders.vue'),
          meta: { title: '系统提醒', icon: 'Bell' }
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/ProfileView.vue'),
          meta: { title: '个人资料', hidden: true }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.name !== 'login' && !userStore.isLoggedIn) {
    next({ name: 'login' })
    return
  }

  if (to.meta?.roles && to.meta.roles.length > 0) {
    if (!userStore.hasRole(to.meta.roles)) {
      next({ name: 'dashboard' })
      return
    }
  }

  next()
})

export default router
