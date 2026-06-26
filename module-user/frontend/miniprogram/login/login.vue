<template>
  <view class="login-page">
    <view class="page-body">
      <view class="hero">
      <view class="logo">校园组队通</view>
      <view class="desc">发现校园活动，结识志同道合的伙伴</view>
    </view>

    <view class="card">
      <view class="tabs">
        <view :class="['tab', mode === 'login' ? 'active' : '']" @click="mode = 'login'">
          <text class="tab-text">登录</text>
        </view>
        <view :class="['tab', mode === 'register' ? 'active' : '']" @click="mode = 'register'">
          <text class="tab-text">注册</text>
        </view>
      </view>

      <view v-if="mode === 'login'" class="form">
        <view class="field">
          <text class="label">账号</text>
          <input class="field-control" v-model="loginForm.username" placeholder="请输入账号" />
        </view>
        <view class="field">
          <text class="label">密码</text>
          <input class="field-control" v-model="loginForm.password" password placeholder="请输入密码" />
        </view>
        <button class="action-btn primary-btn" :loading="loading" @click="handleAccountLogin">登录</button>
      </view>

      <view v-else class="form">
        <view class="field">
          <text class="label">账号</text>
          <input class="field-control" v-model="registerForm.username" placeholder="4-32位字母或数字" />
        </view>
        <view class="field">
          <view class="label-row">
            <text class="label">昵称</text>
            <text class="label-hint">可不填</text>
          </view>
          <input class="field-control" v-model="registerForm.nickname" placeholder="用于展示的名称" />
        </view>
        <view class="field">
          <text class="label">设置密码</text>
          <input class="field-control" v-model="registerForm.password" password placeholder="至少6位" />
        </view>
        <view class="field">
          <text class="label">确认密码</text>
          <input class="field-control" v-model="registerForm.confirmPassword" password placeholder="请再次输入密码" />
        </view>
        <button class="action-btn primary-btn" :loading="loading" @click="handleRegister">注册并登录</button>
      </view>

      <view class="divider">
        <view class="line"></view>
        <text class="divider-text">或</text>
        <view class="line"></view>
      </view>

      <button class="action-btn wx-btn" :loading="wxLoading" @click="handleWxLogin">
        <view class="wx-btn-inner">
          <text class="wx-icon">微</text>
          <text>微信一键登录</text>
        </view>
      </button>
    </view>
    </view>

    <view class="footer-tip">登录即表示同意平台用户协议与隐私政策</view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const mode = ref('login')
const loading = ref(false)
const wxLoading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })

onShow(() => {
  if (uni.getStorageSync('token')) {
    uni.switchTab({ url: '/pages/index/index' })
  }
})

function afterLogin(data) {
  uni.setStorageSync('token', data.token)
  uni.setStorageSync('userId', data.userId)
  if (!data.profileCompleted) {
    uni.showModal({
      title: '提示',
      content: '请先完善个人资料',
      showCancel: false,
      success: () => uni.reLaunch({ url: '/pages/profile/edit' })
    })
  } else {
    uni.switchTab({ url: '/pages/index/index' })
  }
}

async function handleAccountLogin() {
  if (!loginForm.username || !loginForm.password) {
    return uni.showToast({ title: '请填写账号和密码', icon: 'none' })
  }
  loading.value = true
  try {
    const data = await api.login({ username: loginForm.username, password: loginForm.password })
    afterLogin(data)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const { username, nickname, password, confirmPassword } = registerForm
  if (!username || !password) {
    return uni.showToast({ title: '请填写账号和密码', icon: 'none' })
  }
  if (username.length < 4) {
    return uni.showToast({ title: '账号至少4位', icon: 'none' })
  }
  if (password.length < 6) {
    return uni.showToast({ title: '密码至少6位', icon: 'none' })
  }
  if (password !== confirmPassword) {
    return uni.showToast({ title: '两次密码不一致', icon: 'none' })
  }
  loading.value = true
  try {
    const data = await api.register({ username, password, nickname: nickname || undefined })
    uni.showToast({ title: '注册成功', icon: 'success' })
    afterLogin(data)
  } finally {
    loading.value = false
  }
}

function handleWxLogin() {
  wxLoading.value = true
  uni.login({
    provider: 'weixin',
    success: async (res) => {
      try {
        const data = await api.wxLogin(res.code || 'mock_code')
        afterLogin(data)
      } finally {
        wxLoading.value = false
      }
    },
    fail: () => {
      wxLoading.value = false
      uni.showToast({ title: '微信登录失败', icon: 'none' })
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 0 40rpx calc(32rpx + env(safe-area-inset-bottom));
  padding-top: env(safe-area-inset-top);
  background: linear-gradient(180deg, #e8f0ff 0%, #f5f7fb 38%, #f5f6fa 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 120rpx 0 40rpx;
  box-sizing: border-box;
}

.hero {
  text-align: center;
  padding: 0 0 48rpx;
}

.logo {
  font-size: 44rpx;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.3;
  margin-bottom: 20rpx;
}

.desc {
  color: #8a94a6;
  font-size: 26rpx;
  line-height: 1.6;
  padding: 0 24rpx;
}

.card {
  background: #fff;
  border-radius: 28rpx;
  padding: 36rpx 32rpx 40rpx;
  box-shadow: 0 12rpx 40rpx rgba(41, 121, 255, 0.08);
}

.tabs {
  display: flex;
  margin-bottom: 40rpx;
  background: #f3f5f9;
  border-radius: 16rpx;
  padding: 8rpx;
  position: relative;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border-radius: 12rpx;
  color: #8a94a6;
  font-size: 28rpx;
  transition: all 0.2s ease;
}

.tab.active {
  background: #fff;
  color: #2979ff;
  font-weight: 600;
  box-shadow: 0 4rpx 16rpx rgba(41, 121, 255, 0.12);
}

.tab-text {
  line-height: 1.2;
}

.form .field {
  margin-bottom: 28rpx;
}

.form .field:last-of-type {
  margin-bottom: 36rpx;
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.label {
  display: block;
  font-size: 26rpx;
  color: #4a5568;
  margin-bottom: 12rpx;
  font-weight: 500;
}

.label-row .label {
  margin-bottom: 0;
}

.label-hint {
  font-size: 22rpx;
  color: #b0b8c4;
}

.field-control {
  display: block;
  width: 100%;
  min-height: 88rpx;
  padding: 22rpx 24rpx;
  background: #f7f8fb;
  border: 2rpx solid #eef1f6;
  border-radius: 14rpx;
  box-sizing: border-box;
  font-size: 28rpx;
  line-height: 1.5;
  color: #333;
}

.action-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 14rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  padding: 0;
  margin: 0;
}

.action-btn::after {
  border: none;
}

.primary-btn {
  background: linear-gradient(135deg, #3d8bff 0%, #2979ff 100%);
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(41, 121, 255, 0.28);
}

.wx-btn {
  background: #07c160;
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.22);
}

.wx-btn-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 88rpx;
}

.wx-icon {
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.divider {
  display: flex;
  align-items: center;
  margin: 40rpx 0 32rpx;
}

.line {
  flex: 1;
  height: 1rpx;
  background: #e8ebf0;
}

.divider-text {
  flex-shrink: 0;
  width: 72rpx;
  text-align: center;
  color: #b0b8c4;
  font-size: 24rpx;
  line-height: 1;
}

.footer-tip {
  text-align: center;
  color: #c0c6d0;
  font-size: 22rpx;
  line-height: 1.6;
  padding: 24rpx 0 16rpx;
  flex-shrink: 0;
}
</style>
