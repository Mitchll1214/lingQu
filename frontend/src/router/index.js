import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '系统概览' } },
      { path: 'projects', component: () => import('../views/Projects.vue'), meta: { title: '项目管理' } },
      { path: 'datasources', component: () => import('../views/Datasources.vue'), meta: { title: '数据源管理' } },
      { path: 'apis', component: () => import('../views/Apis.vue'), meta: { title: '接口管理' } },
      { path: 'docs', component: () => import('../views/ApiDocs.vue'), meta: { title: '接口文档' } },
      { path: 'debug', component: () => import('../views/ApiDebug.vue'), meta: { title: '在线调试' } },
      { path: 'logs', component: () => import('../views/Logs.vue'), meta: { title: '调用日志' } },
      { path: 'alerts', component: () => import('../views/Alerts.vue'), meta: { title: '告警规则', adminOnly: true } },
      { path: 'users', component: () => import('../views/Users.vue'), meta: { title: '用户管理', adminOnly: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const raw = sessionStorage.getItem('lingqu_user')
  if (to.path !== '/login' && !raw) {
    next('/login')
    return
  }
  // 仅管理员可访问的页面
  if (to.meta?.adminOnly) {
    try {
      const role = JSON.parse(raw || 'null')?.role
      if (role !== 'ADMIN') {
        next('/dashboard')
        return
      }
    } catch (e) {
      next('/login')
      return
    }
  }
  next()
})

export default router
