<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索昵称/学号" style="width:240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="studentNo" label="学号" />
      <el-table-column prop="college" label="学院" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles || []" :key="role" size="small" class="role-tag">
            {{ roleNameMap[role] || role }}
          </el-tag>
          <span v-if="!row.roles?.length" class="no-role">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '正常' : '禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoleDialog(row)">分配角色</el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="disable(row.id)">禁用</el-button>
          <el-button v-else link type="success" @click="enable(row.id)">启用</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" v-model:current-page="page" :page-size="10" :total="total" @current-change="load" />

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <div v-if="currentUser" class="dialog-user">用户：{{ currentUser.nickname }}（ID: {{ currentUser.id }}）</div>
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox v-for="role in assignableRoles" :key="role.roleCode" :label="role.roleCode">
          {{ role.roleName }}
          <span class="role-desc">{{ role.description }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const assignableRoles = ref([])
const roleDialogVisible = ref(false)
const currentUser = ref(null)
const selectedRoles = ref([])

const roleNameMap = {
  USER: '普通师生',
  CREATOR: '活动发起者'
}

onMounted(async () => {
  assignableRoles.value = await adminApi.roles()
  load()
})

async function load() {
  const res = await adminApi.users({ page: page.value, size: 10, keyword: keyword.value })
  list.value = res.records
  total.value = res.total
}

function openRoleDialog(row) {
  currentUser.value = row
  selectedRoles.value = [...(row.roles || [])]
  roleDialogVisible.value = true
}

async function saveRoles() {
  await adminApi.updateUserRoles(currentUser.value.id, { roleCodes: selectedRoles.value })
  ElMessage.success('角色已更新')
  roleDialogVisible.value = false
  load()
}

async function disable(id) {
  await ElMessageBox.confirm('确认禁用该用户？', '提示')
  await adminApi.disableUser(id)
  ElMessage.success('已禁用')
  load()
}

async function enable(id) {
  await ElMessageBox.confirm('确认启用该用户？', '提示')
  await adminApi.enableUser(id)
  ElMessage.success('已启用')
  load()
}
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; }
.role-tag { margin-right: 6px; }
.no-role { color: #999; font-size: 13px; }
.dialog-user { margin-bottom: 16px; color: #606266; }
.role-desc { margin-left: 8px; color: #909399; font-size: 12px; }
.el-checkbox-group { display: flex; flex-direction: column; gap: 12px; }
</style>
