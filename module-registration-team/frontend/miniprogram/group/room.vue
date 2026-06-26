<template>
  <view class="group-room">
    <view class="toolbar" @click="goMembers">
      <text class="member-btn">群成员 ›</text>
    </view>
    <scroll-view scroll-y class="messages" :scroll-into-view="scrollId">
      <view v-for="m in messages" :key="m.id" :id="'msg-' + m.id"
        :class="['msg-row', m.messageType === 2 ? 'system' : (isMine(m) ? 'mine' : 'other')]">
        <view v-if="m.messageType === 2" class="system-msg">{{ m.content }}</view>
        <template v-else>
          <image class="avatar" :src="m.senderAvatar || defaultAvatar" mode="aspectFill" />
          <view class="msg-body">
            <view class="sender-name">{{ m.senderName || '用户' }}</view>
            <view class="bubble">{{ m.content }}</view>
          </view>
        </template>
      </view>
    </scroll-view>
    <view class="input-bar">
      <input v-model="content" placeholder="输入消息" confirm-type="send" @confirm="send" />
      <button size="mini" type="primary" @click="send">发送</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '../../utils/api'

const messages = ref([])
const content = ref('')
const groupId = ref(null)
const activityId = ref(null)
const userId = uni.getStorageSync('userId')
const scrollId = ref('')
const defaultAvatar = '/static/default-avatar.png'

onLoad((q) => {
  groupId.value = q.id
  activityId.value = q.activityId
  uni.setNavigationBarTitle({ title: decodeURIComponent(q.name || '群聊') })
})

onShow(loadHistory)

function isMine(m) {
  return String(m.senderId) === String(userId)
}

function goMembers() {
  uni.navigateTo({ url: `/pages/group/members?groupId=${groupId.value}&activityId=${activityId.value}` })
}

async function loadHistory() {
  const res = await api.groupMessages(groupId.value, 1)
  messages.value = res.records
  if (messages.value.length) {
    scrollId.value = 'msg-' + messages.value[messages.value.length - 1].id
  }
}

async function send() {
  if (!content.value.trim()) return
  await api.sendGroupMessage(groupId.value, content.value)
  content.value = ''
  loadHistory()
}
</script>

<style scoped>
.group-room { display: flex; flex-direction: column; height: 100vh; background: #f5f6fa; }
.toolbar { display: flex; justify-content: flex-end; padding: 16rpx 24rpx; background: #fff; border-bottom: 1rpx solid #eee; }
.member-btn { font-size: 28rpx; color: #2979ff; }
.messages { flex: 1; padding: 24rpx; box-sizing: border-box; }
.msg-row { margin-bottom: 28rpx; }
.msg-row.system { display: flex; justify-content: center; }
.msg-row.other { display: flex; align-items: flex-start; gap: 16rpx; }
.msg-row.mine { display: flex; flex-direction: row-reverse; align-items: flex-start; gap: 16rpx; }
.system-msg { background: rgba(0,0,0,0.06); color: #999; font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 8rpx; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background: #eee; flex-shrink: 0; }
.msg-body { max-width: 70%; min-width: 0; }
.msg-row.mine .msg-body { display: flex; flex-direction: column; align-items: flex-end; }
.sender-name { font-size: 22rpx; color: #999; margin-bottom: 8rpx; line-height: 1.2; }
.bubble { background: #fff; padding: 16rpx 24rpx; border-radius: 16rpx; line-height: 1.5; word-break: break-all; }
.msg-row.mine .bubble { background: #2979ff; color: #fff; }
.input-bar { display: flex; gap: 12rpx; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1rpx solid #eee; }
.input-bar input { flex: 1; background: #f5f6fa; padding: 12rpx 16rpx; border-radius: 8rpx; }
</style>
