<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card" @click="openRoom(item)">
      <view class="row">
        <view class="name">{{ item.peerName }}</view>
        <view v-if="item.unreadCount" class="badge">{{ item.unreadCount }}</view>
      </view>
      <view v-if="item.activityTitle" class="activity-title">{{ item.activityTitle }}</view>
      <view class="text-muted">{{ item.lastMessage }}</view>
    </view>
    <view v-if="!list.length" class="empty">暂无消息</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, updateAllTabBadges } from '../../utils/api'

const list = ref([])

onShow(async () => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  const res = await api.conversations(1)
  list.value = res.records
  updateAllTabBadges()
})

function openRoom(item) {
  uni.navigateTo({ url: `/pages/chat/room?id=${item.id}&peerId=${item.peerId}&activityId=${item.activityId}&peerName=${item.peerName}` })
}
</script>

<style scoped>
.row { display: flex; justify-content: space-between; align-items: center; }
.name { font-weight: 600; font-size: 32rpx; }
.activity-title { color: #666; font-size: 24rpx; margin: 8rpx 0 4rpx; }
.badge { background: #ff4d4f; color: #fff; border-radius: 999rpx; padding: 0 12rpx; font-size: 22rpx; }
.empty { text-align: center; color: #999; padding: 80rpx; }
</style>
