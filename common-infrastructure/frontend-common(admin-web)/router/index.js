import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import Users from '../views/Users.vue'
import Activities from '../views/Activities.vue'
import Complaints from '../views/Complaints.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: Dashboard },
        { path: 'users', component: Users },
        { path: 'activities', component: Activities },
        { path: 'complaints', component: Complaints }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  if (to.path !== '/login' && !localStorage.getItem('adminToken')) {
    next('/login')
  } else {
    next()
  }
})

export default router
