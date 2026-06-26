<template>
  <view class="container">
    <view v-for="m in members" :key="m.userId" class="card member-item">
      <image class="avatar" :src="m.avatar || '/static/default-avatar.png'" mode="aspectFill" />
      <view class="info">
        <view class="name">
          {{ m.nickname }}
          <text v-if="m.role === 1" class="owner-tag">群主</text>
        </view>
        <view class="text-muted">{{ m.college || '未填写学院' }}</view>
      </view>
      <view v-if="String(m.userId) !== String(userId)" class="btn-outline dm-btn" @click="openDm(m)">私信</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const members = ref([])
const groupId = ref(null)
const activityId = ref(null)
const userId = uni.getStorageSync('userId')

onLoad((q) => {
  groupId.value = q.groupId
  activityId.value = q.activityId
})

onShow(async () => {
  members.value = await api.groupMembers(groupId.value)
})

async function openDm(member) {
  const conv = await api.startPrivateChat(Number(activityId.value), member.userId)
  uni.navigateTo({
    url: `/pages/chat/room?id=${conv.id}&peerId=${member.userId}&activityId=${activityId.value}&peerName=${encodeURIComponent(member.nickname)}`
  })
}
</script>

<style scoped>
.member-item { display: flex; align-items: center; gap: 20rpx; }
.avatar { width: 88rpx; height: 88rpx; border-radius: 50%; background: #eee; flex-shrink: 0; }
.info { flex: 1; min-width: 0; }
.name { font-size: 30rpx; font-weight: 600; }
.owner-tag { font-size: 20rpx; color: #2979ff; background: #eef3ff; padding: 2rpx 12rpx; border-radius: 6rpx; margin-left: 8rpx; font-weight: 400; }
.dm-btn { padding: 12rpx 24rpx; font-size: 24rpx; flex-shrink: 0; }
</style>
