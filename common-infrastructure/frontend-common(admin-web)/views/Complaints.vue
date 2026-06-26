<template>
  <el-card>
    <el-tabs v-model="status" @tab-change="load">
      <el-tab-pane label="待处理" name="0" />
      <el-tab-pane label="已处理" name="1" />
      <el-tab-pane label="全部" name="" />
    </el-tabs>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="reporterId" label="举报人" width="100" />
      <el-table-column prop="targetType" label="类型" width="100">
        <template #default="{ row }">{{ typeMap[row.targetType] }}</template>
      </el-table-column>
      <el-table-column prop="targetId" label="目标ID" width="100" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">{{ statusMap[row.status] }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="handle(row.id, true)">通过</el-button>
            <el-button link type="warning" @click="handle(row.id, false)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" v-model:current-page="page" :page-size="10" :total="total" @current-change="load" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/request'
import { ElMessage } from 'element-plus'

const typeMap = { 1: '活动', 2: '评价', 3: '私信', 4: '用户' }
const statusMap = { 0: '待处理', 1: '已处理', 2: '驳回' }
const list = ref([])
const page = ref(1)
const total = ref(0)
const status = ref('0')

onMounted(load)
async function load() {
  const params = { page: page.value, size: 10 }
  if (status.value !== '') params.status = Number(status.value)
  const res = await adminApi.complaints(params)
  list.value = res.records
  total.value = res.total
}

async function handle(id, approved) {
  await adminApi.handleComplaint(id, approved, approved ? '已处理' : '驳回')
  ElMessage.success('操作成功')
  load()
}
</script>

<style scoped>
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
