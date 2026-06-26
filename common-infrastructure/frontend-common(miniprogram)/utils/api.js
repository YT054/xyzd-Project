const BASE_URL = 'http://localhost:8080/api'

function sanitizeQueryParams(data) {
  if (!data || typeof data !== 'object') return data
  return Object.fromEntries(
    Object.entries(data).filter(([, value]) => value != null && value !== '')
  )
}

function handleUnauthorized(message) {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userId')
  uni.showToast({
    title: message || '令牌已过期，请重新登录',
    icon: 'none',
    duration: 2000
  })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/login/login' })
  }, 1500)
}

function request(options) {
  const token = uni.getStorageSync('token')
  const method = options.method || 'GET'
  const data = method === 'GET' ? sanitizeQueryParams(options.data) : options.data
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        if (res.data.code === 200) {
          resolve(res.data.data)
        } else if (res.data.code === 401) {
          handleUnauthorized(res.data.message)
          reject(res.data)
        } else {
          uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        uni.showToast({ title: '网络超时，请稍后重试', icon: 'none' })
        reject(err)
      }
    })
  })
}

/** 本地上传图片 */
function uploadImage(filePath, type = 'common') {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/files/upload',
      filePath,
      name: 'file',
      formData: { type },
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success(res) {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 200) {
            resolve(body.data)
          } else if (body.code === 401) {
            handleUnauthorized(body.message)
            reject(body)
          } else {
            uni.showToast({ title: body.message || '上传失败', icon: 'none' })
            reject(body)
          }
        } catch (e) {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(e)
        }
      },
      fail(err) {
        uni.showToast({ title: '上传失败，请检查网络', icon: 'none' })
        reject(err)
      }
    })
  })
}

/** 从相册选择并上传 */
export function chooseAndUpload(type = 'common') {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        uni.showLoading({ title: '上传中...' })
        try {
          const result = await uploadImage(res.tempFilePaths[0], type)
          resolve(result)
        } catch (e) {
          reject(e)
        } finally {
          uni.hideLoading()
        }
      },
      fail: reject
    })
  })
}

export const api = {
  register: (data) => request({ url: '/auth/register', method: 'POST', data }),
  login: (data) => request({ url: '/auth/login', method: 'POST', data }),
  wxLogin: (code) => request({ url: '/auth/wx-login', method: 'POST', data: { code } }),
  getProfile: () => request({ url: '/auth/profile' }),
  updateProfile: (data) => request({ url: '/auth/profile', method: 'PUT', data }),
  uploadImage,
  chooseAndUpload,
  getCategories: () => request({ url: '/categories' }),
  searchActivities: (params) => request({ url: '/activities/search', data: params }),
  getActivity: (id) => request({ url: '/activities/' + id }),
  publishActivity: (data) => request({ url: '/activities', method: 'POST', data }),
  updateActivity: (data) => request({ url: '/activities', method: 'PUT', data }),
  offlineActivity: (id) => request({ url: '/activities/' + id + '/offline', method: 'PUT' }),
  withdrawActivity: (id) => request({ url: '/activities/' + id + '/withdraw', method: 'PUT' }),
  republishActivity: (id, data) => request({ url: '/activities/' + id + '/republish', method: 'PUT', data }),
  myPublished: (params) => request({ url: '/activities/my-published', data: params }),
  applyRegistration: (data) => request({ url: '/registrations/apply', method: 'POST', data }),
  cancelRegistration: (activityId) => request({ url: '/registrations/cancel/' + activityId, method: 'POST' }),
  auditRegistration: (data) => request({ url: '/registrations/audit', method: 'POST', data }),
  listRegistrations: (activityId, page) => request({ url: '/registrations/activity/' + activityId, data: { page, size: 10 } }),
  myRegistrations: (page, size = 10) => request({ url: '/registrations/my', data: { page, size } }),
  sendMessage: (data) => request({ url: '/chat/send', method: 'POST', data }),
  conversations: (page) => request({ url: '/chat/conversations', data: { page, size: 20 } }),
  chatHistory: (id, page) => request({ url: '/chat/history/' + id, data: { page, size: 50 } }),
  unreadCount: () => request({ url: '/chat/unread-count' }),
  checkin: (data) => request({ url: '/checkin', method: 'POST', data }),
  checkinStats: (activityId) => request({ url: '/checkin/stats/' + activityId }),
  myCheckin: (activityId) => request({ url: '/checkin/my/' + activityId }),
  myCheckins: (page) => request({ url: '/checkin/my', data: { page, size: 20 } }),
  submitReview: (data) => request({ url: '/reviews', method: 'POST', data }),
  activityReviews: (activityId, page) => request({ url: '/reviews/activity/' + activityId, data: { page, size: 10 } }),
  myReviews: (page) => request({ url: '/reviews/my', data: { page, size: 10 } }),
  submitComplaint: (data) => request({ url: '/complaints', method: 'POST', data }),
  createGroup: (data) => request({ url: '/groups', method: 'POST', data }),
  myGroups: (page) => request({ url: '/groups/my', data: { page, size: 20 } }),
  getGroupByActivity: (activityId) => request({ url: '/groups/activity/' + activityId }),
  groupExists: (activityId) => request({ url: '/groups/exists/' + activityId }),
  groupMembers: (groupId) => request({ url: '/groups/' + groupId + '/members' }),
  sendGroupMessage: (groupId, content) => request({ url: '/groups/' + groupId + '/messages', method: 'POST', data: { content } }),
  groupMessages: (groupId, page) => request({ url: '/groups/' + groupId + '/messages', data: { page, size: 50 } }),
  groupUnreadCount: () => request({ url: '/groups/unread-count' }),
  startPrivateChat: (activityId, peerId) => request({ url: '/chat/conversation', data: { activityId, peerId } }),
}

const GROUP_TAB_INDEX = 1
const MESSAGE_TAB_INDEX = 2
const TAB_BAR_ROUTES = new Set([
  'pages/index/index',
  'pages/group/list',
  'pages/chat/list',
  'pages/profile/index'
])

function isTabBarPage() {
  const pages = getCurrentPages()
  if (!pages.length) return false
  return TAB_BAR_ROUTES.has(pages[pages.length - 1].route || '')
}

function safeSetTabBarBadge(options) {
  if (!isTabBarPage()) return
  uni.setTabBarBadge(options)
}

function safeRemoveTabBarBadge(options) {
  if (!isTabBarPage()) return
  uni.removeTabBarBadge(options)
}

export { safeRemoveTabBarBadge }

export async function updateGroupTabBadge() {
  if (!isTabBarPage()) return
  if (!uni.getStorageSync('token')) {
    safeRemoveTabBarBadge({ index: GROUP_TAB_INDEX })
    return
  }
  try {
    const count = await api.groupUnreadCount()
    if (count > 0) {
      safeSetTabBarBadge({ index: GROUP_TAB_INDEX, text: count > 99 ? '99+' : String(count) })
    } else {
      safeRemoveTabBarBadge({ index: GROUP_TAB_INDEX })
    }
  } catch (e) {
    safeRemoveTabBarBadge({ index: GROUP_TAB_INDEX })
  }
}

export async function updateMessageTabBadge() {
  if (!isTabBarPage()) return
  if (!uni.getStorageSync('token')) {
    safeRemoveTabBarBadge({ index: MESSAGE_TAB_INDEX })
    return
  }
  try {
    const count = await api.unreadCount()
    if (count > 0) {
      safeSetTabBarBadge({ index: MESSAGE_TAB_INDEX, text: count > 99 ? '99+' : String(count) })
    } else {
      safeRemoveTabBarBadge({ index: MESSAGE_TAB_INDEX })
    }
  } catch (e) {
    safeRemoveTabBarBadge({ index: MESSAGE_TAB_INDEX })
  }
}

export async function updateAllTabBadges() {
  await Promise.all([updateGroupTabBadge(), updateMessageTabBadge()])
}

export default request
