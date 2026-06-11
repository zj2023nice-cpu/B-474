<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <el-menu
        :default-active="$route.path"
        class="el-menu-vertical"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item
          v-for="item in menuRoutes"
          :key="item.path"
          :index="`/${item.path}`"
        >
          <el-icon>
            <component :is="iconComponents[item.meta.icon]" />
          </el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <span>实验室设备管理系统</span>
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              {{ userStore.user?.name }} ({{ userStore.role }})
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import {
  Odometer,
  OfficeBuilding,
  Monitor,
  List,
  Tools,
  User,
  ArrowDown,
  AlarmClock,
  Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const iconComponents = {
  Odometer,
  OfficeBuilding,
  Monitor,
  List,
  Tools,
  User,
  AlarmClock,
  Bell
}

const layoutRoute = computed(() => {
  return router.options.routes.find(r => r.path === '/')
})

const menuRoutes = computed(() => {
  if (!layoutRoute.value?.children) return []
  return layoutRoute.value.children.filter(route => {
    if (route.meta?.hidden) return false
    if (route.meta?.roles && route.meta.roles.length > 0) {
      return userStore.hasRole(route.meta.roles)
    }
    return true
  })
})

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-menu-vertical {
  height: 100%;
  border-right: none;
}
.header {
  background-color: #fff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  padding: 0 20px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;
}
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}
</style>
