<template>
  <el-card>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="creatorId" label="发起人ID" width="100" />
      <el-table-column prop="activityStatus" label="状态" width="100">
        <template #default="{ row }">{{ statusMap[row.activityStatus] }}</template>
      </el-table-column>
      <el-table-column prop="currentMembers" label="人数" width="80" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.activityStatus !== 4" link type="danger" @click="offline(row.id)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" v-model:current-page="page" :page-size="10" :total="total" @current-change="load" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap = { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已下架' }
const list = ref([])
const page = ref(1)
const total = ref(0)

onMounted(load)
async function load() {
  const res = await adminApi.activities({ page: page.value, size: 10 })
  list.value = res.records
  total.value = res.total
}

async function offline(id) {
  await ElMessageBox.confirm('确认下架该违规活动？', '提示')
  await adminApi.offlineActivity(id)
  ElMessage.success('已下架')
  load()
}
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
