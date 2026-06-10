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
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/labs">
          <el-icon><OfficeBuilding /></el-icon>
          <span>实验室管理</span>
        </el-menu-item>
        <el-menu-item index="/equipments">
          <el-icon><Monitor /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/borrows">
          <el-icon><List /></el-icon>
          <span>借用管理</span>
        </el-menu-item>
        <el-menu-item index="/repairs">
          <el-icon><Tools /></el-icon>
          <span>维修管理</span>
        </el-menu-item>
        <el-menu-item index="/users" v-if="userStore.role === 'ADMIN'">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/expiring" v-if="userStore.role === 'ADMIN' || userStore.role === 'TEACHER'">
          <el-icon><AlarmClock /></el-icon>
          <span>到期提醒</span>
        </el-menu-item>
        <el-menu-item index="/reminders">
          <el-icon><Bell /></el-icon>
          <span>系统提醒</span>
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
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { Odometer, OfficeBuilding, Monitor, List, Tools, ArrowDown, AlarmClock, Bell } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()

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
