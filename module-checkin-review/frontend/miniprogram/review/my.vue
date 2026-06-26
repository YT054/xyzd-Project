<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.activityId)">
      <view class="title">{{ item.activityTitle || '活动评价' }}</view>
      <view class="stars">
        <text v-for="n in 5" :key="n" :class="['star', n <= item.rating ? 'active' : '']">★</text>
      </view>
      <view class="content">{{ item.content }}</view>
      <view class="text-muted">{{ formatTime(item.createdAt) }}</view>
    </view>
    <view v-if="!list.length" class="empty">暂无评价</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const list = ref([])

onShow(async () => {
  const res = await api.myReviews(1)
  list.value = res.records
})

function goDetail(id) { uni.navigateTo({ url: '/pages/activity/detail?id=' + id }) }
function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
</script>

<style scoped>
.title { font-size: 32rpx; font-weight: 600; margin-bottom: 12rpx; }
.stars { margin-bottom: 8rpx; }
.star { font-size: 32rpx; color: #ddd; margin-right: 4rpx; }
.star.active { color: #ffb400; }
.content { line-height: 1.6; margin-bottom: 8rpx; }
.empty { text-align: center; color: #999; padding: 80rpx; }
</style>
