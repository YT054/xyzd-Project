<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card">
      <view class="title" @click="goDetail(item.id)">{{ item.title }}</view>
      <view class="text-muted">{{ statusText(item.activityStatus) }} · {{ item.currentMembers }}人</view>
      <view class="row" v-if="item.activityStatus === 1">
        <view class="btn-outline small" @click.stop="goEdit(item.id)">编辑</view>
        <view class="btn-outline small" @click.stop="withdraw(item.id)">撤回</view>
        <view class="btn-primary small" @click.stop="goManage(item.id)">审核报名</view>
      </view>
      <view class="row" v-else-if="item.activityStatus === 2">
        <view class="btn-outline small" @click.stop="goEdit(item.id)">编辑</view>
        <view class="btn-primary small" @click.stop="goManage(item.id)">审核报名</view>
      </view>
      <view class="row" v-else-if="item.activityStatus === 4">
        <view class="btn-primary small full" @click.stop="goRepublish(item.id)">重新发布</view>
      </view>
    </view>
    <view v-if="!list.length" class="empty">暂无发布</view>
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
  list.value = res.records
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/activity/detail?id=' + id })
}

function goEdit(id) {
  uni.navigateTo({ url: '/pages/activity/publish?id=' + id + '&mode=edit' })
}

function goManage(id) {
  uni.navigateTo({ url: '/pages/activity/manage?id=' + id })
}

function goRepublish(id) {
  uni.navigateTo({ url: '/pages/activity/publish?id=' + id + '&mode=republish' })
}

function withdraw(id) {
  uni.showModal({
    title: '撤回活动',
    content: '撤回后活动将下架，可修改后重新发布，确认撤回？',
    success: async (res) => {
      if (!res.confirm) return
      await api.withdrawActivity(id)
      uni.showToast({ title: '已撤回' })
      load()
    }
  })
}

function statusText(s) {
  return { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已撤回' }[s] || ''
}
</script>

<style scoped>
.title { font-size: 32rpx; font-weight: 600; margin-bottom: 8rpx; }
.row { display: flex; gap: 12rpx; margin-top: 16rpx; }
.small { flex: 1; padding: 12rpx; font-size: 24rpx; text-align: center; }
.full { flex: 1; }
.empty { text-align: center; color: #999; padding: 80rpx; }
</style>
