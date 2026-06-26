<template>
  <view class="container">
    <view class="card" v-if="!submitted">
      <view class="label">评分（1-5星）</view>
      <view class="stars">
        <text v-for="n in 5" :key="n" :class="['star', n <= rating ? 'active' : '']" @click="rating = n">★</text>
      </view>
      <textarea v-model="content" placeholder="写下你的评价" />
      <view class="btn-primary" @click="submit">提交评价</view>
    </view>
    <view class="card" v-else>
      <view class="done-title">已评价</view>
      <view class="stars">
        <text v-for="n in 5" :key="n" :class="['star', n <= rating ? 'active' : '']">★</text>
      </view>
      <view class="content">{{ content }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const activityId = ref(null)
const rating = ref(5)
const content = ref('')
const submitted = ref(false)

onLoad((q) => { activityId.value = q.activityId })
onShow(loadExisting)

async function loadExisting() {
  const res = await api.myReviews(1)
  const existing = (res.records || []).find(r => String(r.activityId) === String(activityId.value))
  if (existing) {
    submitted.value = true
    rating.value = existing.rating
    content.value = existing.content
  }
}

async function submit() {
  if (!content.value.trim()) {
    return uni.showToast({ title: '请填写评价内容', icon: 'none' })
  }
  await api.submitReview({ activityId: Number(activityId.value), rating: rating.value, content: content.value })
  uni.showToast({ title: '评价成功' })
  submitted.value = true
}
</script>

<style scoped>
.label { color: #666; margin-bottom: 8rpx; }
.stars { margin: 16rpx 0 24rpx; }
.star { font-size: 48rpx; color: #ddd; margin-right: 8rpx; }
.star.active { color: #ffb400; }
textarea { background: #f5f6fa; padding: 16rpx; border-radius: 8rpx; width: 100%; min-height: 160rpx; box-sizing: border-box; margin-bottom: 24rpx; }
.done-title { font-size: 32rpx; font-weight: 600; margin-bottom: 12rpx; }
.content { line-height: 1.6; margin-top: 12rpx; }
</style>
