import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/',
      component: () => import('../views/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/Dashboard.vue')
        },
        {
          path: 'labs',
          name: 'labs',
          component: () => import('../views/LabList.vue')
        },
        {
          path: 'equipments',
          name: 'equipments',
          component: () => import('../views/EquipmentList.vue')
        },
        {
          path: 'borrows',
          name: 'borrows',
          component: () => import('../views/BorrowList.vue')
        },
        {
          path: 'repairs',
          name: 'repairs',
          component: () => import('../views/RepairList.vue')
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('../views/UserList.vue'),
          meta: { roles: ['ADMIN'] }
        },
        {
          path: 'expiring',
          name: 'expiring',
          component: () => import('../views/ExpiringEquipments.vue'),
          meta: { roles: ['ADMIN', 'TEACHER'] }
        },
        {
          path: 'reminders',
          name: 'reminders',
          component: () => import('../views/SystemReminders.vue')
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/ProfileView.vue')
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
    const hasRole = to.meta.roles.includes(userStore.role)
    if (!hasRole) {
      next({ name: 'dashboard' })
      return
    }
  }
  next()
})

export default router
