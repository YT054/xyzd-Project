<template>
  <view class="container">
    <view class="card" v-if="!checked && !loading">
      <view class="tip">活动进行中时可提交打卡</view>
      <view class="field"><text class="label">打卡心得</text><textarea v-model="content" placeholder="分享今天的活动感受" /></view>
      <view class="btn-primary" @click="submit">提交打卡</view>
    </view>
    <view class="card" v-else-if="checked">
      <view class="done-title">打卡成功</view>
      <view class="text-muted">打卡时间：{{ formatTime(checked.checkinTime) }}</view>
      <view class="content">{{ checked.content || '无打卡心得' }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const activityId = ref(null)
const content = ref('')
const checked = ref(null)
const loading = ref(true)

onLoad((q) => { activityId.value = q.activityId })
onShow(loadCheckin)

async function loadCheckin() {
  loading.value = true
  try {
    checked.value = await api.myCheckin(activityId.value)
  } finally {
    loading.value = false
  }
}

async function submit() {
  await api.checkin({ activityId: Number(activityId.value), content: content.value })
  uni.showToast({ title: '打卡成功' })
  loadCheckin()
}

function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
</script>

<style scoped>
.tip { color: #999; font-size: 24rpx; margin-bottom: 20rpx; }
.field { margin-bottom: 24rpx; }
.label { display: block; color: #666; margin-bottom: 12rpx; }
textarea { background: #f5f6fa; padding: 16rpx; border-radius: 8rpx; width: 100%; min-height: 160rpx; box-sizing: border-box; }
.done-title { font-size: 32rpx; font-weight: 600; margin-bottom: 12rpx; }
.content { margin-top: 12rpx; line-height: 1.6; }
</style>
