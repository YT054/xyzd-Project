<template>
  <view class="container" v-if="activity">
    <image v-if="activity.coverImage" :src="activity.coverImage" mode="aspectFill" class="cover-banner" />
    <view class="card">
      <view class="title">{{ activity.title }}</view>
      <view class="tag">{{ activity.categoryName }}</view>
      <view class="tag status-tag">{{ statusText(activity.activityStatus) }}</view>
      <view v-if="activity.registerClosed" class="tag closed-tag">已截止报名</view>
      <view class="text-muted">发起人：{{ activity.creatorName }}</view>
      <view class="text-muted">地点：{{ activity.location }}</view>
      <view class="text-muted">时间：{{ formatTime(activity.startTime) }} ~ {{ formatTime(activity.endTime) }}</view>
      <view class="text-muted">报名截止：{{ formatTime(activity.registerDeadline) }}</view>
      <view class="text-muted">人数：{{ activity.currentMembers }}/{{ activity.maxMembers || '不限' }}</view>
      <view class="desc">{{ activity.description }}</view>
    </view>

    <view class="actions" v-if="token && !activity.isCreator">
      <view v-if="!myStatus && !activity.registerClosed" class="btn-primary" @click="apply">立即报名</view>
      <view v-if="!myStatus && activity.registerClosed" class="btn-disabled">已截止报名</view>
      <view v-if="myStatus === 0" class="btn-disabled">待审核</view>
      <view v-if="myStatus === 1" class="btn-disabled">已报名</view>
      <view v-if="canCancel" class="btn-outline" @click="cancel">取消报名</view>
      <view v-if="canCheckin" class="btn-primary" @click="goCheckin">去打卡</view>
      <view v-if="myStatus === 1 && activity.activityStatus === 2 && activity.myHasCheckin" class="btn-disabled">已打卡</view>
      <view v-if="canReview" class="btn-primary" @click="goReview">去评价</view>
      <view v-if="myStatus === 1 && activity.activityStatus === 3 && activity.myHasReview" class="btn-disabled">已评价</view>
      <view v-if="myStatus === 1 && activity.inGroup" class="btn-primary" @click="goGroup">进入群聊</view>
      <view v-if="myStatus === 1 && activity.activityStatus !== 4" class="btn-outline" @click="goChat">私信消息</view>
    </view>

    <view class="actions" v-if="token && activity.isCreator">
      <view class="btn-primary" @click="goManage">管理报名</view>
      <view v-if="activity.inGroup" class="btn-primary" @click="goGroup">进入群聊</view>
      <view v-else class="btn-outline" @click="goCreateGroup">建立群聊</view>
    </view>

    <view v-if="!token" class="btn-primary" @click="goLogin">登录后报名</view>

    <view class="card">
      <view class="section-title">活动评价</view>
      <view v-for="r in reviews" :key="r.id" class="review-item">
        <view>{{ r.nickname }} · {{ r.rating }}星</view>
        <view class="text-muted">{{ r.content }}</view>
      </view>
      <view v-if="!reviews.length" class="text-muted">暂无评价</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const activity = ref(null)
const reviews = ref([])
const activityId = ref(null)
const token = uni.getStorageSync('token')
const myStatus = computed(() => activity.value?.myRegistrationStatus)
const canCancel = computed(() => {
  const a = activity.value
  if (!a) return false
  return (myStatus.value === 0 || myStatus.value === 1) && a.activityStatus === 1
})
const canCheckin = computed(() => {
  const a = activity.value
  return a && myStatus.value === 1 && a.activityStatus === 2 && !a.myHasCheckin
})
const canReview = computed(() => {
  const a = activity.value
  return a && myStatus.value === 1 && a.activityStatus === 3 && !a.myHasReview
})

onLoad((q) => { activityId.value = q.id })
onShow(loadData)

async function loadData() {
  activity.value = await api.getActivity(activityId.value)
  const res = await api.activityReviews(activityId.value, 1)
  reviews.value = res.records
}

async function apply() {
  await api.applyRegistration({ activityId: activityId.value, applyMessage: '' })
  uni.showToast({ title: '报名成功，等待审核' })
  loadData()
}

function cancel() {
  uni.showModal({
    title: '取消报名',
    content: '活动开始前可取消报名，确认取消？',
    success: async (res) => {
      if (!res.confirm) return
      await api.cancelRegistration(activityId.value)
      uni.showToast({ title: '已取消' })
      loadData()
    }
  })
}

function goManage() { uni.navigateTo({ url: '/pages/activity/manage?id=' + activityId.value }) }
function goCheckin() { uni.navigateTo({ url: '/pages/checkin/index?activityId=' + activityId.value }) }
function goReview() { uni.navigateTo({ url: '/pages/review/index?activityId=' + activityId.value }) }
function goGroup() {
  uni.navigateTo({
    url: `/pages/group/room?id=${activity.value.groupId}&activityId=${activityId.value}&name=${encodeURIComponent(activity.value.title + ' 交流群')}`
  })
}
function goCreateGroup() {
  uni.navigateTo({ url: '/pages/group/create' })
}
function goChat() { uni.switchTab({ url: '/pages/chat/list' }) }
function goLogin() { uni.navigateTo({ url: '/pages/login/login' }) }

function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function statusText(s) { return { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已下架' }[s] || '' }
</script>

<style scoped>
.cover-banner { width: 100%; height: 360rpx; border-radius: 16rpx; margin-bottom: 20rpx; }
.title { font-size: 36rpx; font-weight: 600; margin-bottom: 12rpx; }
.status-tag { background: #f0f0f0; color: #666; }
.closed-tag { background: #f5f5f5; color: #999; }
.desc { margin-top: 16rpx; line-height: 1.6; }
.actions { display: flex; flex-direction: column; gap: 16rpx; margin: 24rpx 0; }
.section-title { font-weight: 600; margin-bottom: 16rpx; }
.review-item { padding: 12rpx 0; border-bottom: 1rpx solid #f0f0f0; }
</style>
