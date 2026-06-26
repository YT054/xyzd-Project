<template>
  <view class="container">
    <view class="search-bar">
      <input v-model="keyword" placeholder="搜索活动关键词" confirm-type="search" @confirm="loadData(true)" />
      <button size="mini" type="primary" @click="loadData(true)">搜索</button>
    </view>
    <scroll-view scroll-x class="category-scroll">
      <view :class="['cat-item', !categoryId ? 'active' : '']" @click="selectCategory(null)">全部</view>
      <view v-for="c in categories" :key="c.id" :class="['cat-item', categoryId === c.id ? 'active' : '']" @click="selectCategory(c.id)">{{ c.name }}</view>
    </scroll-view>
    <view class="filter-row">
      <picker :range="statusLabels" @change="onStatusChange">
        <view class="filter-item">状态：{{ statusLabels[statusIndex] }}</view>
      </picker>
    </view>
    <view v-for="item in list" :key="item.id" class="card activity-card" @click="goDetail(item.id)">
      <image v-if="item.coverImage" :src="item.coverImage" mode="aspectFill" class="cover-thumb" />
      <view class="title">{{ item.title }}</view>
      <view class="text-muted">{{ item.categoryName }} · {{ item.location }}</view>
      <view class="text-muted">{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</view>
      <view class="footer">
        <view class="tags">
          <text class="tag">{{ statusText(item.activityStatus) }}</text>
          <text v-if="item.registerClosed" class="tag closed-tag">已截止报名</text>
        </view>
        <text class="text-muted">{{ item.currentMembers }}/{{ item.maxMembers || '不限' }}人</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty">暂无活动</view>
    <view v-if="hasMore" class="load-more" @click="loadData(false)">加载更多</view>
    <view v-if="showFab" class="fab" @click="onFabClick">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api, updateAllTabBadges } from '../../utils/api'

const keyword = ref('')
const categoryId = ref(null)
const categories = ref([])
const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const statusIndex = ref(0)
const isCreator = ref(false)
const checkinActivities = ref([])
const hasApprovedRegistration = ref(false)
const token = ref(uni.getStorageSync('token'))
const showFab = computed(() => token.value && (isCreator.value || hasApprovedRegistration.value))
const statusLabels = ['全部状态', '招募中', '进行中', '已结束']
const statusValues = [null, 1, 2, 3]

onMounted(async () => {
  categories.value = await api.getCategories()
  loadData(true)
})

onShow(() => {
  token.value = uni.getStorageSync('token')
  loadData(true)
  updateAllTabBadges()
  loadUserRole()
  loadCheckinActivities()
})

async function loadCheckinActivities() {
  if (!token.value) {
    checkinActivities.value = []
    hasApprovedRegistration.value = false
    return
  }
  try {
    const res = await api.myRegistrations(1, 50)
    const approved = (res.records || []).filter((item) => item.status === 1)
    hasApprovedRegistration.value = approved.length > 0
    checkinActivities.value = approved.filter((item) => item.activityStatus === 2)
    console.log('[打卡排查] 我的报名原始数据:', res.records)
    console.log('[打卡排查] 已通过报名:', approved)
    console.log('[打卡排查] 可打卡活动(activityStatus=2):', checkinActivities.value)
  } catch (e) {
    checkinActivities.value = []
    hasApprovedRegistration.value = false
  }
}

async function loadUserRole() {
  if (!uni.getStorageSync('token')) {
    isCreator.value = false
    return
  }
  try {
    const profile = await api.getProfile()
    isCreator.value = (profile.roles || []).includes('CREATOR')
  } catch (e) {
    isCreator.value = false
  }
}

function selectCategory(id) {
  categoryId.value = id
  loadData(true)
}

function onStatusChange(e) {
  statusIndex.value = Number(e.detail.value)
  loadData(true)
}

async function loadData(reset) {
  if (reset) { page.value = 1; list.value = [] }
  const res = await api.searchActivities({
    keyword: keyword.value,
    categoryId: categoryId.value,
    activityStatus: statusValues[statusIndex.value],
    page: page.value,
    size: 10
  })
  list.value = reset ? res.records : list.value.concat(res.records)
  hasMore.value = list.value.length < res.total
  page.value++
}

function goDetail(id) {
  uni.navigateTo({ url: '/pages/activity/detail?id=' + id })
}

function onFabClick() {
  if (isCreator.value) {
    showCreatorMenu()
    return
  }
  showCheckinPicker()
}

function showCreatorMenu() {
  const itemList = []
  const handlers = []
  if (checkinActivities.value.length) {
    itemList.push('活动打卡')
    handlers.push(showCheckinPicker)
  }
  itemList.push('发布活动', '建立群聊')
  handlers.push(goPublish, goCreateGroup)
  uni.showActionSheet({
    itemList,
    success(res) {
      const handler = handlers[res.tapIndex]
      if (handler) handler()
    }
  })
}

function showCheckinPicker() {
  console.log('[打卡排查] 点击打卡, checkinActivities=', checkinActivities.value)
  if (!checkinActivities.value.length) {
    return uni.showToast({ title: '活动尚未开始，暂不可打卡', icon: 'none' })
  }
  if (checkinActivities.value.length === 1) {
    goCheckin(checkinActivities.value[0].activityId)
    return
  }
  uni.showActionSheet({
    itemList: checkinActivities.value.map((item) => item.activityTitle),
    success(res) {
      const act = checkinActivities.value[res.tapIndex]
      if (act) goCheckin(act.activityId)
    }
  })
}

function goCheckin(activityId) {
  uni.navigateTo({ url: '/pages/checkin/index?activityId=' + activityId })
}

function goPublish() {
  uni.navigateTo({ url: '/pages/activity/publish' })
}

function goCreateGroup() {
  uni.navigateTo({ url: '/pages/group/create' })
}

function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function statusText(s) {
  return { 1: '招募中', 2: '进行中', 3: '已结束', 4: '已下架' }[s] || ''
}
</script>

<style scoped>
.search-bar { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.search-bar input { flex: 1; background: #fff; padding: 16rpx 24rpx; border-radius: 12rpx; }
.category-scroll { white-space: nowrap; margin-bottom: 20rpx; }
.cat-item { display: inline-block; padding: 12rpx 28rpx; margin-right: 12rpx; background: #fff; border-radius: 999rpx; }
.cat-item.active { background: #2979ff; color: #fff; }
.filter-row { margin-bottom: 16rpx; }
.activity-card .title { font-size: 32rpx; font-weight: 600; margin-bottom: 8rpx; }
.cover-thumb { width: 100%; height: 240rpx; border-radius: 12rpx; margin-bottom: 12rpx; }
.footer { display: flex; justify-content: space-between; margin-top: 12rpx; align-items: center; }
.tags { display: flex; gap: 8rpx; flex-wrap: wrap; }
.closed-tag { background: #f5f5f5; color: #999; }
.empty, .load-more { text-align: center; color: #999; padding: 40rpx; }
.fab {
  position: fixed;
  right: 32rpx;
  bottom: calc(120rpx + env(safe-area-inset-bottom));
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: #2979ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 28rpx rgba(41, 121, 255, 0.45);
  z-index: 100;
}
.fab-icon { font-size: 60rpx; line-height: 1; font-weight: 300; margin-top: -4rpx; }
</style>
