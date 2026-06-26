<template>
  <view class="container">
    <view class="tip card">选择要绑定群聊的活动，已通过审核的成员将自动加入</view>
    <view v-for="item in list" :key="item.id" class="card" @click="create(item)">
      <view class="title">{{ item.title }}</view>
      <view class="text-muted">{{ statusText(item.activityStatus) }} · {{ item.currentMembers }}人已报名</view>
    </view>
    <view v-if="!list.length" class="empty">暂无可建群的活动</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const list = ref([])

onShow(load)

async function load() {
  const res = await api.myPublished({ page: 1, size: 50 })
  const activities = res.records.filter(a => a.activityStatus !== 4)
  const available = []
  for (const act of activities) {
    const exists = await api.groupExists(act.id)
    if (!exists) available.push(act)
  }
  list.value = available
}

async function create(item) {
  uni.showModal({
    title: '建立群聊',
    content: `为「${item.title}」建立交流群？`,
    success: async (res) => {
      if (!res.confirm) return
      const group = await api.createGroup({ activityId: item.id })
      uni.showToast({ title: '群聊已创建' })
      setTimeout(() => {
        uni.redirectTo({
          url: `/pages/group/room?id=${group.id}&activityId=${item.id}&name=${encodeURIComponent(group.name)}`
        })
      }, 400)
    }
  })
}

function statusText(s) { return { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已下架' }[s] || '' }
</script>

<style scoped>
.tip { color: #666; font-size: 26rpx; line-height: 1.6; margin-bottom: 20rpx; }
.title { font-size: 32rpx; font-weight: 600; }
.empty { text-align: center; color: #999; padding: 80rpx; }
</style>
