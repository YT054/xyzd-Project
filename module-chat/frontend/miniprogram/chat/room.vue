<template>
  <view class="container chat-room">
    <scroll-view scroll-y class="messages" :scroll-into-view="scrollId">
      <view v-for="m in messages" :key="m.id" :id="'msg-' + m.id" :class="['msg', m.senderId == userId ? 'mine' : '']">
        <view class="bubble">{{ m.content }}</view>
      </view>
    </scroll-view>
    <view class="input-bar">
      <input v-model="content" placeholder="输入消息" />
      <button size="mini" type="primary" @click="send">发送</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const messages = ref([])
const content = ref('')
const conversationId = ref(null)
const peerId = ref(null)
const activityId = ref(null)
const userId = uni.getStorageSync('userId')
const scrollId = ref('')

onLoad((q) => {
  conversationId.value = q.id
  peerId.value = q.peerId
  activityId.value = q.activityId
  uni.setNavigationBarTitle({ title: q.peerName || '私信' })
})

onMounted(loadHistory)

async function loadHistory() {
  const res = await api.chatHistory(conversationId.value, 1)
  messages.value = res.records
  if (messages.value.length) scrollId.value = 'msg-' + messages.value[messages.value.length - 1].id
}

async function send() {
  if (!content.value.trim()) return
  await api.sendMessage({ activityId: activityId.value, receiverId: Number(peerId.value), content: content.value })
  content.value = ''
  loadHistory()
}
</script>

<style scoped>
.chat-room { display: flex; flex-direction: column; height: 100vh; padding: 0; }
.messages { flex: 1; padding: 24rpx; box-sizing: border-box; }
.msg { display: flex; margin-bottom: 16rpx; }
.msg.mine { justify-content: flex-end; }
.bubble { max-width: 70%; background: #fff; padding: 16rpx 24rpx; border-radius: 16rpx; }
.msg.mine .bubble { background: #2979ff; color: #fff; }
.input-bar { display: flex; gap: 12rpx; padding: 16rpx; background: #fff; border-top: 1rpx solid #eee; }
.input-bar input { flex: 1; background: #f5f6fa; padding: 12rpx 16rpx; border-radius: 8rpx; }
</style>
