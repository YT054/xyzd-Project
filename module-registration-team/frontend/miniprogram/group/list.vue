<template>
  <view class="container">
    <view v-for="item in list" :key="item.id" class="card" @click="openGroup(item)">
      <view class="row">
        <view class="name">{{ item.name }}</view>
        <view v-if="item.unreadCount" class="badge">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</view>
      </view>
      <view class="text-muted">{{ item.activityTitle }}</view>
      <view class="meta">
        <text class="text-muted">{{ item.lastMessage }}</text>
        <text class="text-muted count">{{ item.memberCount }}人</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty">暂无群聊，发起者可从首页 + 建立群聊</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, updateGroupTabBadge } from '../../utils/api'

const list = ref([])

onShow(async () => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  const res = await api.myGroups(1)
  list.value = res.records
  updateGroupTabBadge()
})

function openGroup(item) {
  uni.navigateTo({
    url: `/pages/group/room?id=${item.id}&activityId=${item.activityId}&name=${encodeURIComponent(item.name)}`
  })
}
</script>

<style scoped>
.row { display: flex; justify-content: space-between; align-items: center; }
.name { font-weight: 600; font-size: 32rpx; }
.badge { background: #ff4d4f; color: #fff; border-radius: 999rpx; padding: 0 12rpx; font-size: 22rpx; }
.meta { display: flex; justify-content: space-between; margin-top: 8rpx; gap: 16rpx; }
.count { flex-shrink: 0; }
.empty { text-align: center; color: #999; padding: 80rpx; line-height: 1.6; }
</style>
