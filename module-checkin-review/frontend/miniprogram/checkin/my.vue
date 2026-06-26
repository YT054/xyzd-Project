<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.activityId)">
      <view class="title">{{ item.activityTitle }}</view>
      <view class="meta">
        <view class="tag">{{ actStatusText(item.activityStatus) }}</view>
      </view>
      <view class="text-muted">打卡时间：{{ formatTime(item.checkinTime) }}</view>
      <view class="content">{{ item.content || '无打卡心得' }}</view>
    </view>
    <view v-if="!list.length" class="empty">暂无打卡记录</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const list = ref([])

onShow(async () => {
  const res = await api.myCheckins(1)
  list.value = res.records
})

function goDetail(id) { uni.navigateTo({ url: '/pages/activity/detail?id=' + id }) }
function actStatusText(s) { return { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已下架' }[s] || '' }
function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
</script>

<style scoped>
.title { font-size: 32rpx; font-weight: 600; margin-bottom: 12rpx; }
.meta { margin-bottom: 8rpx; }
.content { margin-top: 8rpx; line-height: 1.6; color: #333; }
.empty { text-align: center; color: #999; padding: 80rpx; }
</style>
