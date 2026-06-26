import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

function handleUnauthorized(message) {
  localStorage.removeItem('adminToken')
  ElMessage.warning(message || '令牌已过期，请重新登录')
  router.push('/login')
}

request.interceptors.request.use(config => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

request.interceptors.response.use(
  res => {
    if (res.data.code === 401) {
      handleUnauthorized(res.data.message)
      return Promise.reject(res.data)
    }
    if (res.data.code === 200) return res.data.data
    ElMessage.error(res.data.message || '请求失败')
    return Promise.reject(res.data)
  },
  err => {
    if (err.response?.status === 401) {
      handleUnauthorized(err.response?.data?.message)
    } else {
      ElMessage.error(err.response?.data?.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default request

export const adminApi = {
  login: (data) => request.post('/admin/login', data),
  stats: () => request.get('/admin/stats'),
  users: (params) => request.get('/admin/users', { params }),
  roles: () => request.get('/admin/roles'),
  updateUserRoles: (id, data) => request.put('/admin/users/' + id + '/roles', data),
  disableUser: (id) => request.put('/admin/users/' + id + '/disable'),
  enableUser: (id) => request.put('/admin/users/' + id + '/enable'),
  activities: (params) => request.get('/admin/activities', { params }),
  offlineActivity: (id) => request.put('/admin/activities/' + id + '/offline'),
  complaints: (params) => request.get('/admin/complaints', { params }),
  handleComplaint: (id, approved, remark) => request.put('/admin/complaints/' + id + '/handle', null, { params: { approved, remark } }),
  deleteReview: (id) => request.delete('/admin/reviews/' + id)
}
