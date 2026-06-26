<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card">
      <view class="row">
        <view>
          <view class="name">{{ item.nickname }}</view>
          <view class="text-muted">{{ item.college }}</view>
          <view class="text-muted">{{ item.applyMessage }}</view>
        </view>
        <view class="status">{{ statusText(item.status) }}</view>
      </view>
      <view v-if="item.status === 0" class="actions">
        <view class="btn-primary small" @click="audit(item.id, true)">通过</view>
        <view class="btn-outline small" @click="audit(item.id, false)">拒绝</view>
      </view>
    </view>
    <view class="card">
      <view class="section-title">打卡统计</view>
      <view v-for="d in stats.details" :key="d.userId" class="text-muted">{{ d.nickname }} - {{ d.checkinTime ? '已打卡' : '未打卡' }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const list = ref([])
const stats = ref({ details: [] })
const activityId = ref(null)

onLoad((q) => { activityId.value = q.id })

onMounted(async () => {
  const res = await api.listRegistrations(activityId.value, 1)
  list.value = res.records
  stats.value = await api.checkinStats(activityId.value)
})

async function audit(regId, approved) {
  await api.auditRegistration({ registrationId: regId, approved, auditMessage: approved ? '欢迎加入' : '抱歉未能通过' })
  uni.showToast({ title: '已处理' })
  const res = await api.listRegistrations(activityId.value, 1)
  list.value = res.records
}

function statusText(s) { return { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已取消' }[s] || '' }
</script>

<style scoped>
.row { display: flex; justify-content: space-between; }
.name { font-weight: 600; }
.actions { display: flex; gap: 16rpx; margin-top: 16rpx; }
.small { flex: 1; padding: 12rpx; font-size: 24rpx; }
.section-title { font-weight: 600; margin-bottom: 12rpx; }
</style>
