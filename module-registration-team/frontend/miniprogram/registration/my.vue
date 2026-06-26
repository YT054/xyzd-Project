<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card">
      <view class="title" @click="goDetail(item.activityId)">{{ item.activityTitle }}</view>
      <view class="meta">
        <view class="tag">{{ regStatusText(item.status) }}</view>
        <view class="tag gray">{{ actStatusText(item.activityStatus) }}</view>
      </view>
      <view class="text-muted" v-if="item.startTime">开始时间：{{ formatTime(item.startTime) }}</view>
      <view class="row" v-if="showActions(item)">
        <view v-if="canCancel(item)" class="btn-outline small" @click.stop="cancel(item.activityId)">取消报名</view>
        <view v-if="canCheckin(item)" class="btn-primary small" @click.stop="goCheckin(item.activityId)">去打卡</view>
        <view v-if="canReview(item)" class="btn-primary small" @click.stop="goReview(item.activityId)">去评价</view>
      </view>
    </view>
    <view v-if="!list.length" class="empty">暂无报名</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const list = ref([])

onShow(load)

async function load() {
  const res = await api.myRegistrations(1)
  list.value = res.records
}

function goDetail(id) { uni.navigateTo({ url: '/pages/activity/detail?id=' + id }) }
function goCheckin(id) { uni.navigateTo({ url: '/pages/checkin/index?activityId=' + id }) }
function goReview(id) { uni.navigateTo({ url: '/pages/review/index?activityId=' + id }) }

function canCancel(item) {
  return (item.status === 0 || item.status === 1) && item.activityStatus === 1
}
function canCheckin(item) {
  return item.status === 1 && item.activityStatus === 2
}
function canReview(item) {
  return item.status === 1 && item.activityStatus === 3
}
function showActions(item) {
  return canCancel(item) || canCheckin(item) || canReview(item)
}

function cancel(activityId) {
  uni.showModal({
    title: '取消报名',
    content: '活动开始前可取消报名，确认取消？',
    success: async (res) => {
      if (!res.confirm) return
      await api.cancelRegistration(activityId)
      uni.showToast({ title: '已取消' })
      load()
    }
  })
}

function regStatusText(s) { return { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已取消' }[s] || '' }
function actStatusText(s) { return { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已下架' }[s] || '' }
function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
</script>

<style scoped>
.title { font-size: 32rpx; font-weight: 600; margin-bottom: 12rpx; }
.meta { display: flex; gap: 12rpx; margin-bottom: 8rpx; }
.tag.gray { background: #f0f0f0; color: #666; }
.row { display: flex; gap: 16rpx; margin-top: 16rpx; }
.small { flex: 1; padding: 12rpx; font-size: 24rpx; }
.empty { text-align: center; color: #999; padding: 80rpx; }
</style>
