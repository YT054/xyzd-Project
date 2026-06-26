<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">校园组队通</div>
      <el-menu :default-active="route.path" router background-color="#001529" text-color="#fff" active-text-color="#409eff">
        <el-menu-item index="/dashboard">数据统计</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/activities">活动监管</el-menu-item>
        <el-menu-item index="/complaints">投诉处理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ adminName }}（{{ roleCode }}）</span>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const adminName = localStorage.getItem('adminName')
const roleCode = localStorage.getItem('adminRole')

function logout() {
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.aside { background: #001529; min-height: 100vh; }
.logo { color: #fff; font-size: 18px; font-weight: 700; padding: 20px; text-align: center; }
.header { display: flex; justify-content: space-between; align-items: center; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
</style>
