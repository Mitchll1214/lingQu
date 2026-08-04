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
      { path: 'apis', component: () => import('../views/Apis.vue'), meta: { title: '接口管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = sessionStorage.getItem('lingqu_user')
  if (to.path !== '/login' && !user) {
    next('/login')
  } else {
    next()
  }
})

export default router
