<template>
  <view class="container">
    <view class="card form">
      <view class="field">
        <text class="label">头像</text>
        <view class="avatar-upload" @click="pickAvatar">
          <image :src="avatarPreview || '/static/default-avatar.png'" mode="aspectFill" class="avatar-img" />
          <text class="avatar-tip">点击更换</text>
        </view>
      </view>
      <view class="field"><text class="label">昵称*</text><input class="field-control" v-model="form.nickname" placeholder="请输入昵称" /></view>
      <view class="field"><text class="label">学号*</text><input class="field-control" v-model="form.studentNo" placeholder="请输入学号" /></view>
      <view class="field"><text class="label">学院*</text><input class="field-control" v-model="form.college" placeholder="请输入学院" /></view>
      <view class="field"><text class="label">手机号</text><input class="field-control" v-model="form.phone" placeholder="选填" /></view>
      <view class="field">
        <text class="label">隐私设置</text>
        <picker :range="privacyLabels" @change="e => form.privacyLevel = Number(e.detail.value)">
          <view class="field-control picker-value">{{ privacyLabels[form.privacyLevel] }}</view>
        </picker>
      </view>
    </view>
    <view class="btn-primary" :class="{ disabled: saving }" @click="submit">{{ saving ? '保存中...' : '保存' }}</view>
  </view>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { api } from '../../utils/api'

const privacyLabels = ['公开', '仅同活动可见', '私密']
const avatarPreview = ref('')
const saving = ref(false)
const form = reactive({ nickname: '', studentNo: '', college: '', phone: '', avatar: '', gender: 0, privacyLevel: 0 })

onMounted(async () => {
  const p = await api.getProfile()
  Object.assign(form, {
    nickname: p.nickname, studentNo: p.studentNo, college: p.college,
    phone: p.phone, avatar: p.avatar, gender: p.gender || 0, privacyLevel: p.privacyLevel || 0
  })
  avatarPreview.value = p.avatar
})

async function pickAvatar() {
  try {
    const result = await api.chooseAndUpload('avatar')
    form.avatar = result.path
    avatarPreview.value = result.url
  } catch (e) {
    // 用户取消
  }
}

async function submit() {
  if (saving.value) return
  if (!form.nickname || !form.studentNo || !form.college) {
    return uni.showToast({ title: '请填写必填项', icon: 'none' })
  }
  saving.value = true
  try {
    await api.updateProfile(form)
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => {
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.switchTab({ url: '/pages/index/index' })
      }
    }, 500)
  } finally {
    saving.value = false
  }
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
.picker-value { display: flex; align-items: center; }
.avatar-upload { display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.avatar-img { width: 160rpx; height: 160rpx; border-radius: 50%; background: #eee; }
.avatar-tip { font-size: 24rpx; color: #2979ff; }
.btn-primary.disabled { opacity: 0.6; }
</style>
