<template>
  <view class="container">
    <view class="card profile-header" v-if="profile">
      <image class="avatar" :src="profile.avatar || '/static/default-avatar.png'" mode="aspectFill" />
      <view class="info">
        <view class="name">{{ profile.nickname }}</view>
        <view class="text-muted">{{ profile.college || '未填写学院' }}</view>
      </view>
    </view>
    <view class="card menu-list">
      <view class="menu-item" @click="go('/pages/profile/edit')">编辑资料</view>
      <view class="menu-item" @click="switchTab('/pages/chat/list')">我的消息</view>
      <view class="menu-item" @click="go('/pages/registration/my')">我的报名</view>
      <view class="menu-item" @click="go('/pages/checkin/my')">我的打卡</view>
      <view class="menu-item" @click="go('/pages/review/my')">我的评价</view>
      <view v-if="isCreator" class="menu-item" @click="go('/pages/activity/publish')">发布活动</view>
      <view v-if="isCreator" class="menu-item" @click="go('/pages/activity/my-publish')">我的发布</view>
    </view>
    <view class="btn-outline logout" @click="logout">退出登录</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, safeRemoveTabBarBadge, updateAllTabBadges } from '../../utils/api'

const profile = ref(null)
const isCreator = ref(false)

onShow(async () => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  profile.value = await api.getProfile()
  isCreator.value = (profile.value.roles || []).includes('CREATOR')
  updateAllTabBadges()
})

function go(url) { uni.navigateTo({ url }) }
function switchTab(url) { uni.switchTab({ url }) }
function logout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success(res) {
      if (!res.confirm) return
      uni.removeStorageSync('token')
      uni.removeStorageSync('userId')
      safeRemoveTabBarBadge({ index: 1 })
      safeRemoveTabBarBadge({ index: 2 })
      uni.reLaunch({ url: '/pages/login/login' })
    }
  })
}
</script>

<style scoped>
.profile-header { display: flex; align-items: center; gap: 24rpx; }
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; background: #eee; }
.name { font-size: 36rpx; font-weight: 600; }
.menu-item { padding: 28rpx 0; border-bottom: 1rpx solid #f0f0f0; display: flex; justify-content: space-between; }
.logout { margin-top: 40rpx; }
</style>
