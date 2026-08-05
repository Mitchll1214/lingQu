import request from './index'

export const authApi = {
  login: (data) => request.post('/api/admin/auth/login', data),
  logout: () => request.post('/api/admin/auth/logout'),
  me: () => request.get('/api/admin/auth/me')
}

export const dashboardApi = {
  stats: () => request.get('/api/admin/dashboard/stats')
}

export const projectApi = {
  page: (params) => request.get('/api/admin/projects', { params }),
  options: () => request.get('/api/admin/projects/options'),
  create: (data) => request.post('/api/admin/projects', data),
  update: (id, data) => request.put(`/api/admin/projects/${id}`, data),
  updateStatus: (id, status) => request.put(`/api/admin/projects/${id}/status`, { status }),
  remove: (id) => request.delete(`/api/admin/projects/${id}`)
}

export const datasourceApi = {
  page: (params) => request.get('/api/admin/datasources', { params }),
  options: () => request.get('/api/admin/datasources/options'),
  create: (data) => request.post('/api/admin/datasources', data),
  update: (id, data) => request.put(`/api/admin/datasources/${id}`, data),
  test: (id) => request.post(`/api/admin/datasources/${id}/test`),
  remove: (id) => request.delete(`/api/admin/datasources/${id}`)
}

export const apiApi = {
  page: (params) => request.get('/api/admin/apis', { params }),
  create: (data) => request.post('/api/admin/apis', data),
  update: (id, data) => request.put(`/api/admin/apis/${id}`, data),
  updateStatus: (id, status) => request.put(`/api/admin/apis/${id}/status`, { status }),
  remove: (id) => request.delete(`/api/admin/apis/${id}`)
}

export const tokenApi = {
  list: (projectId) => request.get('/api/admin/tokens', { params: { projectId } }),
  create: (data) => request.post('/api/admin/tokens', data),
  revoke: (id) => request.delete(`/api/admin/tokens/${id}`)
}

export const logApi = {
  page: (params) => request.get('/api/admin/logs', { params })
}

export const debugApi = {
  execute: (data) => request.post('/api/admin/debug/execute', data),
  executorUrl: () => request.get('/api/admin/debug/executor-url')
}

export const alertApi = {
  page: (params) => request.get('/api/admin/alerts', { params }),
  create: (data) => request.post('/api/admin/alerts', data),
  update: (id, data) => request.put(`/api/admin/alerts/${id}`, data),
  updateStatus: (id, status) => request.put(`/api/admin/alerts/${id}/status`, { status }),
  remove: (id) => request.delete(`/api/admin/alerts/${id}`)
}
