import api from './request'

export const dataDevelopmentApi = {
  // 任务列表
  getTasks: (params) => api.get('/data-development/tasks', { params }),
  // 文件夹树
  getFolders: () => api.get('/data-development/folders'),
  // 脚本详情
  getScript: (id) => api.get(`/data-development/script/${id}`),
  // 保存脚本
  saveScript: (data) => api.post('/data-development/script/save', data),
  // 执行SQL
  execute: (data) => api.post('/data-development/execute', data),
  // 依赖关系
  getDependencies: (taskId, taskType) => api.get(`/data-development/dependencies/${taskId}`, { params: { taskType } }),
  refreshDependencies: () => api.post('/data-development/dependencies/refresh'),
  // 执行历史
  getExecutions: (taskId, taskType, limit) => api.get(`/data-development/executions/${taskId}`, { params: { taskType, limit } }),
  // 调度配置
  updateSchedule: (id, data, taskType) => api.put(`/data-development/schedule/${id}`, data, { params: { taskType } }),
  // 差异对比
  diff: (devScriptId) => api.get(`/data-development/diff/${devScriptId}`),
  // 灰度测试
  grayscaleTest: (data) => api.post('/data-development/grayscale-test', data),
  // 发布
  publish: (data) => api.post('/data-development/publish', data),
  // 发布历史
  getPublishHistory: (devScriptId) => api.get(`/data-development/publish-history/${devScriptId}`),
  getAllPublishHistory: (params) => api.get('/data-development/publish-history/all', { params }),
  // 回滚
  rollback: (publishId, data) => api.post(`/data-development/rollback/${publishId}`, data)
}

export const datasourceApi = {
  list: () => api.get('/datasource/list'),
  test: (data) => api.post('/datasource/test', data),
  save: (data) => api.post('/datasource/save', data),
  delete: (id) => api.delete(`/datasource/${id}`)
}

export const syncTaskApi = {
  list: () => api.get('/datax/sync-tasks'),
  create: (data) => api.post('/datax/sync-task/create', data),
  update: (data) => api.post('/datax/sync-task/update', data),
  delete: (id) => api.post(`/datax/sync-task/delete/${id}`),
  run: (id) => api.post('/datax/run', { syncTaskId: id })
}