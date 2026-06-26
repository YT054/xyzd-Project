<template>
  <div class="login-page" :class="{ 'is-form-open': showForm }">
    <div class="cover-layer" :class="{ blurred: showForm }">
      <img :src="loginCover" alt="校园组队通" class="cover-img" />
    </div>
    <div class="cover-dim" :class="{ visible: showForm }"></div>

    <transition name="link-fade">
      <button
        v-if="!showForm && showLoginLink"
        type="button"
        class="login-trigger"
        @click="openForm"
      >
        点此登录
      </button>
    </transition>

    <transition name="form-fade">
      <el-card v-if="showForm" class="login-card">
        <h2>校园组队通管理后台</h2>
        <el-form :model="form" @submit.prevent="submit">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="admin" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="admin123" show-password />
          </el-form-item>
          <el-button type="primary" style="width:100%" @click="submit">登录</el-button>
        </el-form>
      </el-card>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../api/request'
import loginCover from '../img/登录页.png'

const router = useRouter()
const form = reactive({ username: 'admin', password: 'admin123' })
const showLoginLink = ref(false)
const showForm = ref(false)
let linkTimer = null

onMounted(() => {
  linkTimer = window.setTimeout(() => {
    showLoginLink.value = true
  }, 1500)
})

onUnmounted(() => {
  if (linkTimer) window.clearTimeout(linkTimer)
})

function openForm() {
  showForm.value = true
}

async function submit() {
  const data = await adminApi.login(form)
  localStorage.setItem('adminToken', data.token)
  localStorage.setItem('adminRole', data.roleCode)
  localStorage.setItem('adminName', data.realName || data.username)
  router.push('/dashboard')
}
</script>

<style scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #0f172a;
}

.cover-layer {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: filter 0.6s ease, transform 0.6s ease;
  will-change: filter, transform;
}

.cover-layer.blurred {
  filter: blur(14px);
  transform: scale(1.06);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  user-select: none;
  pointer-events: none;
}

.cover-dim {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  opacity: 0;
  transition: opacity 0.6s ease;
  pointer-events: none;
}

.cover-dim.visible {
  opacity: 1;
}

.login-trigger {
  position: absolute;
  left: 50%;
  bottom: 12%;
  transform: translateX(-50%);
  z-index: 2;
  padding: 12px 36px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #1d4ed8;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
  cursor: pointer;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  transition: transform 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
}

.login-trigger:hover {
  transform: translateX(-50%) translateY(-2px);
  background: #fff;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.22);
}

.login-card {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 3;
  width: min(400px, calc(100vw - 48px));
  backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.96) !important;
}

h2 {
  text-align: center;
  margin-bottom: 24px;
}

.link-fade-enter-active,
.link-fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.link-fade-enter-from,
.link-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(12px);
}

.form-fade-enter-active {
  transition: opacity 0.45s ease, transform 0.45s ease;
}

.form-fade-enter-from {
  opacity: 0;
  transform: translate(-50%, calc(-50% + 20px));
}
</style>
