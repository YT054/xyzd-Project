<template>
  <view class="container">
    <view class="card form">
      <view class="field">
        <text class="label">封面图</text>
        <view class="cover-upload" @click="pickCover">
          <image v-if="coverPreview" :src="coverPreview" mode="aspectFill" class="cover-img" />
          <view v-else class="cover-placeholder">
            <text class="plus">+</text>
            <text class="tip">从相册选择封面</text>
          </view>
        </view>
      </view>
      <view class="field"><text class="label">标题*</text><input class="field-control" v-model="form.title" /></view>
      <view class="field">
        <text class="label">分类*</text>
        <picker :range="categories" range-key="name" @change="onCategoryChange">
          <view class="field-control picker-value">{{ selectedCategory || '请选择' }}</view>
        </picker>
      </view>
      <view class="field"><text class="label">地点</text><input class="field-control" v-model="form.location" /></view>

      <view class="field">
        <text class="label">开始时间*</text>
        <view class="datetime-row">
          <picker class="datetime-picker" mode="date" :value="startDate" :start="minDate" @change="onStartDateChange">
            <view class="field-control picker-value" :class="{ placeholder: !startDate }">{{ startDate || '选择日期' }}</view>
          </picker>
          <picker class="datetime-picker" mode="time" :value="startClock" @change="onStartClockChange">
            <view class="field-control picker-value" :class="{ placeholder: !startClock }">{{ startClock || '选择时间' }}</view>
          </picker>
        </view>
      </view>

      <view class="field">
        <text class="label">结束时间*</text>
        <view class="datetime-row">
          <picker class="datetime-picker" mode="date" :value="endDate" :start="minDate" @change="onEndDateChange">
            <view class="field-control picker-value" :class="{ placeholder: !endDate }">{{ endDate || '选择日期' }}</view>
          </picker>
          <picker class="datetime-picker" mode="time" :value="endClock" @change="onEndClockChange">
            <view class="field-control picker-value" :class="{ placeholder: !endClock }">{{ endClock || '选择时间' }}</view>
          </picker>
        </view>
      </view>

      <view class="field">
        <text class="label">报名截止*</text>
        <view class="datetime-row">
          <picker class="datetime-picker" mode="date" :value="deadlineDate" :start="minDate" @change="onDeadlineDateChange">
            <view class="field-control picker-value" :class="{ placeholder: !deadlineDate }">{{ deadlineDate || '选择日期' }}</view>
          </picker>
          <picker class="datetime-picker" mode="time" :value="deadlineClock" @change="onDeadlineClockChange">
            <view class="field-control picker-value" :class="{ placeholder: !deadlineClock }">{{ deadlineClock || '选择时间' }}</view>
          </picker>
        </view>
        <text class="form-tip">不能晚于活动开始时间；早于当前时间则视为已截止报名</text>
      </view>

      <view class="field"><text class="label">人数上限*</text><input class="field-control" v-model="form.maxMembers" type="number" /></view>
      <view class="field"><text class="label">描述*</text><textarea class="field-control textarea" v-model="form.description" /></view>
    </view>
    <view class="btn-primary" @click="submit">{{ submitLabel }}</view>
  </view>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const categories = ref([])
const selectedCategory = ref('')
const coverPreview = ref('')
const minDate = ref(getTodayDate())
const startDate = ref('')
const startClock = ref('')
const endDate = ref('')
const endClock = ref('')
const deadlineDate = ref('')
const deadlineClock = ref('')
const activityId = ref(null)
const isEdit = ref(false)
const isRepublish = ref(false)
const submitLabel = computed(() => {
  if (isEdit.value) return '保存修改'
  if (isRepublish.value) return '重新发布'
  return '发布活动'
})
const form = reactive({
  categoryId: null, title: '', description: '', location: '', coverImage: '',
  startTime: '', endTime: '', registerDeadline: '', maxMembers: 10, tags: ''
})

function pad(n) {
  return String(n).padStart(2, '0')
}

function getTodayDate() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function syncDateTime(dateRef, clockRef, targetKey) {
  if (dateRef.value && clockRef.value) {
    form[targetKey] = `${dateRef.value} ${clockRef.value}:00`
  } else {
    form[targetKey] = ''
  }
}

function onStartDateChange(e) { startDate.value = e.detail.value; syncDateTime(startDate, startClock, 'startTime') }
function onStartClockChange(e) { startClock.value = e.detail.value; syncDateTime(startDate, startClock, 'startTime') }
function onEndDateChange(e) { endDate.value = e.detail.value; syncDateTime(endDate, endClock, 'endTime') }
function onEndClockChange(e) { endClock.value = e.detail.value; syncDateTime(endDate, endClock, 'endTime') }
function onDeadlineDateChange(e) { deadlineDate.value = e.detail.value; syncDateTime(deadlineDate, deadlineClock, 'registerDeadline') }
function onDeadlineClockChange(e) { deadlineClock.value = e.detail.value; syncDateTime(deadlineDate, deadlineClock, 'registerDeadline') }

function splitDateTime(value) {
  if (!value) return { date: '', clock: '' }
  const normalized = value.replace('T', ' ')
  const [date, time] = normalized.split(' ')
  return { date: date || '', clock: (time || '').substring(0, 5) }
}

function fillDateTimeFields() {
  const start = splitDateTime(form.startTime)
  startDate.value = start.date
  startClock.value = start.clock
  const end = splitDateTime(form.endTime)
  endDate.value = end.date
  endClock.value = end.clock
  const deadline = splitDateTime(form.registerDeadline)
  deadlineDate.value = deadline.date
  deadlineClock.value = deadline.clock
}

onLoad((query) => {
  if (query.id) {
    activityId.value = query.id
    if (query.mode === 'edit') {
      isEdit.value = true
      uni.setNavigationBarTitle({ title: '编辑活动' })
    } else if (query.mode === 'republish') {
      isRepublish.value = true
      uni.setNavigationBarTitle({ title: '重新发布' })
    }
  }
})

onMounted(async () => {
  categories.value = await api.getCategories()
  if (activityId.value) {
    const activity = await api.getActivity(activityId.value)
    form.categoryId = activity.categoryId
    form.title = activity.title
    form.description = activity.description
    form.location = activity.location || ''
    form.coverImage = activity.coverImage || ''
    form.startTime = activity.startTime
    form.endTime = activity.endTime
    form.registerDeadline = activity.registerDeadline
    form.maxMembers = activity.maxMembers
    form.tags = activity.tags || ''
    coverPreview.value = activity.coverImage || ''
    selectedCategory.value = activity.categoryName || ''
    fillDateTimeFields()
  }
})

function onCategoryChange(e) {
  const idx = e.detail.value
  form.categoryId = categories.value[idx].id
  selectedCategory.value = categories.value[idx].name
}

async function pickCover() {
  try {
    const result = await api.chooseAndUpload('activity')
    form.coverImage = result.path
    coverPreview.value = result.url
  } catch (e) {
    // 用户取消选择
  }
}

async function submit() {
  if (!form.title || !form.description || !form.categoryId || !form.startTime || !form.endTime || !form.registerDeadline) {
    return uni.showToast({ title: '请填写完整信息', icon: 'none' })
  }
  form.maxMembers = Number(form.maxMembers)
  if (isEdit.value) {
    await api.updateActivity({ ...form, id: Number(activityId.value) })
    uni.showToast({ title: '保存成功' })
  } else if (isRepublish.value) {
    await api.republishActivity(activityId.value, form)
    uni.showToast({ title: '重新发布成功' })
  } else {
    await api.publishActivity(form)
    uni.showToast({ title: '发布成功' })
  }
  setTimeout(() => uni.navigateBack(), 500)
}
</script>

<style scoped>
.field { margin-bottom: 24rpx; }
.label { display: block; color: #666; margin-bottom: 12rpx; font-size: 28rpx; }
.field-control {
  display: block;
  width: 100%;
  min-height: 88rpx;
  padding: 22rpx 24rpx;
  background: #f5f6fa;
  border-radius: 8rpx;
  box-sizing: border-box;
  font-size: 28rpx;
  line-height: 1.5;
  color: #333;
}
.field-control.textarea {
  min-height: 200rpx;
  line-height: 1.6;
}
.datetime-row {
  display: flex;
  gap: 16rpx;
}
.datetime-picker {
  flex: 1;
  min-width: 0;
}
.picker-value {
  display: flex;
  align-items: center;
  color: #333;
}
.picker-value.placeholder { color: #999; }
.form-tip { display: block; margin-top: 8rpx; font-size: 22rpx; color: #999; line-height: 1.5; }
.cover-upload { width: 100%; height: 320rpx; border-radius: 12rpx; overflow: hidden; background: #f5f6fa; }
.cover-img { width: 100%; height: 100%; }
.cover-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; }
.plus { font-size: 64rpx; line-height: 1; }
.tip { font-size: 24rpx; margin-top: 8rpx; }
</style>
