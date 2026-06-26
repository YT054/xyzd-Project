<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6"><el-card><div class="stat-title">注册用户</div><div class="stat-num">{{ stats.userCount || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-title">活动总数</div><div class="stat-num">{{ stats.activityCount || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-title">有效评价</div><div class="stat-num">{{ stats.reviewCount || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat-title">待处理投诉</div><div class="stat-num warn">{{ stats.pendingComplaints || 0 }}</div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/request'

const stats = ref({})
onMounted(async () => { stats.value = await adminApi.stats() })
</script>

<style scoped>
.stat-title { color: #666; margin-bottom: 8px; }
.stat-num { font-size: 32px; font-weight: 700; }
.warn { color: #f56c6c; }
</style>
